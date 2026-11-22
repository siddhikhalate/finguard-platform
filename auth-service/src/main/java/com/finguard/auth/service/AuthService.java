package com.finguard.auth.service;

import com.finguard.auth.dto.AuthResponse;
import com.finguard.auth.dto.LoginRequest;
import com.finguard.auth.dto.RegisterRequest;
import com.finguard.auth.dto.TokenRefreshRequest;
import com.finguard.auth.dto.TokenRefreshResponse;
import com.finguard.auth.dto.UserSummaryDto;
import com.finguard.auth.entity.RefreshToken;
import com.finguard.auth.entity.Role;
import com.finguard.auth.entity.User;
import com.finguard.auth.exception.AuthException;
import com.finguard.auth.exception.ResourceNotFoundException;
import com.finguard.auth.exception.TokenRefreshException;
import com.finguard.auth.repository.RefreshTokenRepository;
import com.finguard.auth.repository.RoleRepository;
import com.finguard.auth.repository.UserRepository;
import com.finguard.auth.security.JwtTokenProvider;
import com.finguard.auth.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.time.Instant;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    public static final int MAX_FAILED_LOGIN_ATTEMPTS = 5;
    public static final int LOCKOUT_DURATION_MINUTES = 15;

    private final AuthenticationManager authenticationManager;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider tokenProvider;

    @Value("${finguard.security.jwt.refresh-token-expiration-ms:604800000}")
    private long refreshTokenExpirationMs;

    @Transactional(noRollbackFor = AuthException.class)
    public AuthResponse login(LoginRequest loginRequest) {
        String username = loginRequest.getUsername().trim().toLowerCase();
        Optional<User> userOpt = userRepository.findByUsername(username);

        // 1. Check account lockout state
        if (userOpt.isPresent()) {
            User user = userOpt.get();
            if (user.getLockedUntil() != null) {
                if (user.getLockedUntil().isAfter(Instant.now())) {
                    long minutesLeft = Duration.between(Instant.now(), user.getLockedUntil()).toMinutes() + 1;
                    log.warn("Login attempt for locked account {}: {} minutes remaining", username, minutesLeft);
                    throw new AuthException("Account is temporarily locked due to multiple failed login attempts. Try again in " + minutesLeft + " minutes.");
                } else {
                    // Lockout expired; reset lock
                    user.setLockedUntil(null);
                    user.setFailedLoginAttempts(0);
                    userRepository.save(user);
                }
            }
        }

        // 2. Perform authentication with lockout counter increment on failure
        Authentication authentication;
        try {
            authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            username,
                            loginRequest.getPassword()
                    )
            );
        } catch (BadCredentialsException ex) {
            if (userOpt.isPresent()) {
                User user = userOpt.get();
                int attempts = user.getFailedLoginAttempts() + 1;
                user.setFailedLoginAttempts(attempts);
                if (attempts >= MAX_FAILED_LOGIN_ATTEMPTS) {
                    user.setLockedUntil(Instant.now().plus(Duration.ofMinutes(LOCKOUT_DURATION_MINUTES)));
                    log.warn("Account {} locked for {} minutes after {} failed login attempts",
                            username, LOCKOUT_DURATION_MINUTES, attempts);
                }
                userRepository.save(user);
            }
            throw new AuthException("Invalid username or password");
        }

        // 3. Reset failed login attempts on successful authentication
        if (userOpt.isPresent()) {
            User user = userOpt.get();
            if (user.getFailedLoginAttempts() > 0 || user.getLockedUntil() != null) {
                user.setFailedLoginAttempts(0);
                user.setLockedUntil(null);
                userRepository.save(user);
            }
        }

        SecurityContextHolder.getContext().setAuthentication(authentication);
        UserPrincipal userPrincipal = (UserPrincipal) authentication.getPrincipal();

        String accessToken = tokenProvider.generateAccessToken(userPrincipal);
        String rawRefreshToken = UUID.randomUUID().toString();
        createRefreshToken(userPrincipal.getId(), rawRefreshToken);

        Set<String> roles = userPrincipal.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toSet());

        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(rawRefreshToken)
                .tokenType("Bearer")
                .expiresIn(tokenProvider.getExpirationMs() / 1000)
                .userId(userPrincipal.getId())
                .username(userPrincipal.getUsername())
                .email(userPrincipal.getEmail())
                .roles(roles)
                .build();
    }

    @Transactional
    public AuthResponse register(RegisterRequest registerRequest) {
        if (userRepository.existsByUsername(registerRequest.getUsername())) {
            throw new AuthException("Username is already taken");
        }

        if (userRepository.existsByEmail(registerRequest.getEmail())) {
            throw new AuthException("Email is already registered");
        }

        // Privilege Escalation Prevention: Self-registration strictly assigns ROLE_USER
        Role userRole = roleRepository.findByName("ROLE_USER")
                .orElseThrow(() -> new ResourceNotFoundException("Role", "name", "ROLE_USER"));
        Set<Role> roles = Set.of(userRole);

        User user = User.builder()
                .username(registerRequest.getUsername().trim().toLowerCase())
                .email(registerRequest.getEmail().trim().toLowerCase())
                .passwordHash(passwordEncoder.encode(registerRequest.getPassword()))
                .firstName(registerRequest.getFirstName())
                .lastName(registerRequest.getLastName())
                .isEnabled(true)
                .failedLoginAttempts(0)
                .roles(roles)
                .build();

        User savedUser = userRepository.save(user);
        log.info("Registered new user: {} with roles: {}", savedUser.getUsername(), roles.stream().map(Role::getName).toList());

        UserPrincipal principal = UserPrincipal.create(savedUser);
        String accessToken = tokenProvider.generateAccessToken(principal);
        String rawRefreshToken = UUID.randomUUID().toString();
        createRefreshToken(savedUser.getId(), rawRefreshToken);

        Set<String> roleNames = roles.stream().map(Role::getName).collect(Collectors.toSet());

        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(rawRefreshToken)
                .tokenType("Bearer")
                .expiresIn(tokenProvider.getExpirationMs() / 1000)
                .userId(savedUser.getId())
                .username(savedUser.getUsername())
                .email(savedUser.getEmail())
                .roles(roleNames)
                .build();
    }

    @Transactional
    public TokenRefreshResponse refreshToken(TokenRefreshRequest request) {
        String requestRefreshToken = request.getRefreshToken();
        String tokenHash = hashToken(requestRefreshToken);

        // Verify token strictly against database hash lookup (no plaintext fallback)
        return refreshTokenRepository.findByTokenHash(tokenHash)
                .map(this::verifyExpiration)
                .map(RefreshToken::getUser)
                .map(user -> {
                    UserPrincipal principal = UserPrincipal.create(user);
                    String newAccessToken = tokenProvider.generateAccessToken(principal);
                    return TokenRefreshResponse.builder()
                            .accessToken(newAccessToken)
                            .refreshToken(requestRefreshToken)
                            .tokenType("Bearer")
                            .expiresIn(tokenProvider.getExpirationMs() / 1000)
                            .build();
                })
                .orElseThrow(() -> new TokenRefreshException(tokenHash, "Refresh token not found in database"));
    }

    @Transactional(readOnly = true)
    public UserSummaryDto getCurrentUser(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));

        Set<String> roles = user.getRoles().stream()
                .map(Role::getName)
                .collect(Collectors.toSet());

        return UserSummaryDto.builder()
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .isEnabled(user.isEnabled())
                .roles(roles)
                .createdAt(user.getCreatedAt())
                .build();
    }

    private RefreshToken createRefreshToken(UUID userId, String rawToken) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));

        // Delete existing refresh tokens for the user to maintain single active session per device
        refreshTokenRepository.deleteByUser(user);

        String tokenHash = hashToken(rawToken);

        RefreshToken refreshToken = RefreshToken.builder()
                .user(user)
                .tokenHash(tokenHash)
                .expiryDate(Instant.now().plusMillis(refreshTokenExpirationMs))
                .revoked(false)
                .build();

        return refreshTokenRepository.save(refreshToken);
    }

    private RefreshToken verifyExpiration(RefreshToken token) {
        if (token.isRevoked()) {
            throw new TokenRefreshException(token.getTokenHash(), "Refresh token was revoked");
        }

        if (token.getExpiryDate().isBefore(Instant.now())) {
            refreshTokenRepository.delete(token);
            throw new TokenRefreshException(token.getTokenHash(), "Refresh token has expired. Please log in again.");
        }

        return token;
    }

    @Transactional
    public AuthResponse updateUserRoles(UUID userId, Set<String> roleNames) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));

        if (roleNames == null || roleNames.isEmpty()) {
            throw new AuthException("At least one role must be specified");
        }

        Set<Role> roles = new HashSet<>();
        for (String roleName : roleNames) {
            String normalizedRole = roleName.startsWith("ROLE_") ? roleName : "ROLE_" + roleName;
            Role role = roleRepository.findByName(normalizedRole)
                    .orElseThrow(() -> new ResourceNotFoundException("Role", "name", normalizedRole));
            roles.add(role);
        }

        user.setRoles(roles);
        User updatedUser = userRepository.save(user);
        log.info("Admin updated roles for user {}: {}", updatedUser.getUsername(), roleNames);

        UserPrincipal principal = UserPrincipal.create(updatedUser);
        String accessToken = tokenProvider.generateAccessToken(principal);

        return AuthResponse.builder()
                .userId(updatedUser.getId())
                .username(updatedUser.getUsername())
                .email(updatedUser.getEmail())
                .roles(roles.stream().map(Role::getName).collect(Collectors.toSet()))
                .accessToken(accessToken)
                .tokenType("Bearer")
                .expiresIn(tokenProvider.getExpirationMs() / 1000)
                .build();
    }

    public static String hashToken(String rawToken) {
        if (rawToken == null) {
            return null;
        }
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(rawToken.getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 algorithm unavailable", e);
        }
    }
}

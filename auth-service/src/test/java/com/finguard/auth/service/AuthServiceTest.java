package com.finguard.auth.service;

import com.finguard.auth.dto.AuthResponse;
import com.finguard.auth.dto.LoginRequest;
import com.finguard.auth.dto.RegisterRequest;
import com.finguard.auth.entity.RefreshToken;
import com.finguard.auth.entity.Role;
import com.finguard.auth.entity.User;
import com.finguard.auth.exception.AuthException;
import com.finguard.auth.repository.RefreshTokenRepository;
import com.finguard.auth.repository.RoleRepository;
import com.finguard.auth.repository.UserRepository;
import com.finguard.auth.security.JwtTokenProvider;
import com.finguard.auth.security.UserPrincipal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private UserRepository userRepository;

    @Mock
    private RoleRepository roleRepository;

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtTokenProvider tokenProvider;

    @InjectMocks
    private AuthService authService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(authService, "refreshTokenExpirationMs", 604800000L);
    }

    @Test
    @DisplayName("Should successfully authenticate user and return AuthResponse")
    void shouldLoginSuccessfully() {
        UUID userId = UUID.randomUUID();
        User user = User.builder()
                .id(userId)
                .username("analyst_jane")
                .email("jane@finguard.internal")
                .passwordHash("encoded_pwd")
                .roles(Set.of(Role.builder().id(2).name("ROLE_ANALYST").build()))
                .build();

        UserPrincipal principal = UserPrincipal.create(user);
        Authentication auth = new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());

        when(userRepository.findByUsername("analyst_jane")).thenReturn(Optional.of(user));
        when(authenticationManager.authenticate(any())).thenReturn(auth);
        when(tokenProvider.generateAccessToken(any())).thenReturn("mock.jwt.token");
        when(tokenProvider.getExpirationMs()).thenReturn(86400000L);
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(refreshTokenRepository.save(any())).thenAnswer(invocation -> {
            RefreshToken token = invocation.getArgument(0);
            token.setId(UUID.randomUUID());
            return token;
        });

        LoginRequest request = LoginRequest.builder()
                .username("analyst_jane")
                .password("Password@123")
                .build();

        AuthResponse response = authService.login(request);

        assertNotNull(response);
        assertEquals("mock.jwt.token", response.getAccessToken());
        assertEquals("analyst_jane", response.getUsername());
        assertTrue(response.getRoles().contains("ROLE_ANALYST"));
        verify(refreshTokenRepository).deleteByUser(user);
    }

    @Test
    @DisplayName("Should lock account for 15 minutes after 5 consecutive failed login attempts")
    void shouldLockAccountAfter5FailedAttempts() {
        UUID userId = UUID.randomUUID();
        User user = User.builder()
                .id(userId)
                .username("target_user")
                .failedLoginAttempts(4)
                .build();

        when(userRepository.findByUsername("target_user")).thenReturn(Optional.of(user));
        when(authenticationManager.authenticate(any()))
                .thenThrow(new org.springframework.security.authentication.BadCredentialsException("Bad credentials"));

        LoginRequest request = LoginRequest.builder()
                .username("target_user")
                .password("WrongPassword")
                .build();

        // 5th failed attempt should trigger lockout
        assertThrows(AuthException.class, () -> authService.login(request));
        assertEquals(5, user.getFailedLoginAttempts());
        assertNotNull(user.getLockedUntil());
        assertTrue(user.getLockedUntil().isAfter(Instant.now()));

        // Subsequent attempt while locked should be immediately rejected without hitting AuthenticationManager
        AuthException lockException = assertThrows(AuthException.class, () -> authService.login(request));
        assertTrue(lockException.getMessage().contains("Account is temporarily locked"));
    }

    @Test
    @DisplayName("Should hash refresh token with SHA-256 before saving to database")
    void shouldHashRefreshTokenInDatabase() {
        UUID userId = UUID.randomUUID();
        User user = User.builder()
                .id(userId)
                .username("secure_user")
                .build();

        UserPrincipal principal = UserPrincipal.create(user);
        Authentication auth = new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());

        when(userRepository.findByUsername("secure_user")).thenReturn(Optional.of(user));
        when(authenticationManager.authenticate(any())).thenReturn(auth);
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(tokenProvider.generateAccessToken(any())).thenReturn("access.token");
        when(tokenProvider.getExpirationMs()).thenReturn(3600000L);

        org.mockito.ArgumentCaptor<RefreshToken> captor = org.mockito.ArgumentCaptor.forClass(RefreshToken.class);
        when(refreshTokenRepository.save(captor.capture())).thenAnswer(inv -> inv.getArgument(0));

        LoginRequest request = LoginRequest.builder()
                .username("secure_user")
                .password("ValidPassword")
                .build();

        authService.login(request);

        RefreshToken savedToken = captor.getValue();
        assertNotNull(savedToken.getTokenHash());
        assertEquals(64, savedToken.getTokenHash().length());
        assertEquals(AuthService.hashToken(savedToken.getToken()), savedToken.getTokenHash());
    }

    @Test
    @DisplayName("Should throw AuthException when registering with duplicate username")
    void shouldFailRegisterWhenUsernameExists() {
        when(userRepository.existsByUsername("existing_user")).thenReturn(true);

        RegisterRequest request = RegisterRequest.builder()
                .username("existing_user")
                .email("new@finguard.internal")
                .password("Password@123")
                .build();

        AuthException exception = assertThrows(AuthException.class, () -> authService.register(request));
        assertEquals("Username is already taken", exception.getMessage());
        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should register new user successfully with ROLE_USER default")
    void shouldRegisterNewUserSuccessfully() {
        UUID userId = UUID.randomUUID();
        Role userRole = Role.builder().id(3).name("ROLE_USER").build();

        when(userRepository.existsByUsername("new_trader")).thenReturn(false);
        when(userRepository.existsByEmail("trader@finguard.internal")).thenReturn(false);
        when(roleRepository.findByName("ROLE_USER")).thenReturn(Optional.of(userRole));
        when(passwordEncoder.encode("Password@123")).thenReturn("encodedPassword");

        User savedUser = User.builder()
                .id(userId)
                .username("new_trader")
                .email("trader@finguard.internal")
                .passwordHash("encodedPassword")
                .roles(Set.of(userRole))
                .isEnabled(true)
                .build();

        when(userRepository.save(any(User.class))).thenReturn(savedUser);
        when(userRepository.findById(userId)).thenReturn(Optional.of(savedUser));
        when(tokenProvider.generateAccessToken(any())).thenReturn("token_123");
        when(tokenProvider.getExpirationMs()).thenReturn(86400000L);
        when(refreshTokenRepository.save(any())).thenReturn(RefreshToken.builder()
                .id(UUID.randomUUID())
                .token("refresh_abc")
                .expiryDate(Instant.now().plusMillis(604800000))
                .build());

        RegisterRequest request = RegisterRequest.builder()
                .username("new_trader")
                .email("trader@finguard.internal")
                .password("Password@123")
                .build();

        AuthResponse response = authService.register(request);

        assertNotNull(response);
        assertEquals("new_trader", response.getUsername());
        assertTrue(response.getRoles().contains("ROLE_USER"));
        verify(userRepository).save(any(User.class));
    }

    @Test
    @DisplayName("Should successfully update user roles when called by admin")
    void shouldUpdateUserRolesSuccessfully() {
        UUID userId = UUID.randomUUID();
        User existingUser = User.builder()
                .id(userId)
                .username("trader_bob")
                .email("bob@finguard.internal")
                .roles(Set.of(Role.builder().id(3).name("ROLE_USER").build()))
                .isEnabled(true)
                .build();

        Role analystRole = Role.builder().id(2).name("ROLE_ANALYST").build();

        when(userRepository.findById(userId)).thenReturn(Optional.of(existingUser));
        when(roleRepository.findByName("ROLE_ANALYST")).thenReturn(Optional.of(analystRole));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(tokenProvider.generateAccessToken(any())).thenReturn("new.admin.granted.token");

        AuthResponse response = authService.updateUserRoles(userId, Set.of("ANALYST"));

        assertNotNull(response);
        assertEquals(userId, response.getUserId());
        assertTrue(response.getRoles().contains("ROLE_ANALYST"));
        verify(userRepository).save(existingUser);
    }

    @Test
    @DisplayName("Should fail updating user roles when role set is empty")
    void shouldFailUpdatingUserRolesWhenRolesEmpty() {
        UUID userId = UUID.randomUUID();
        User existingUser = User.builder()
                .id(userId)
                .username("trader_bob")
                .email("bob@finguard.internal")
                .build();

        when(userRepository.findById(userId)).thenReturn(Optional.of(existingUser));

        assertThrows(AuthException.class, () -> authService.updateUserRoles(userId, Set.of()));
        verify(userRepository, never()).save(any());
    }
}

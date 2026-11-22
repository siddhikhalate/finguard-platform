package com.finguard.auth.service;

import com.finguard.auth.dto.LoginRequest;
import com.finguard.auth.entity.Role;
import com.finguard.auth.entity.User;
import com.finguard.auth.exception.AuthException;
import com.finguard.auth.repository.RoleRepository;
import com.finguard.auth.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
public class AuthServiceLockoutIntegrationTest {

    @Autowired
    private AuthService authService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private static final String USERNAME = "lockout_test_user";
    private static final String CORRECT_PASSWORD = "CorrectPassword@123";
    private static final String WRONG_PASSWORD = "WrongPassword@999";

    @BeforeEach
    void setUp() {
        userRepository.deleteAll();
        Role userRole = roleRepository.findByName("ROLE_USER")
                .orElseGet(() -> roleRepository.save(Role.builder().name("ROLE_USER").build()));

        User user = User.builder()
                .username(USERNAME)
                .email("lockout_test@finguard.local")
                .passwordHash(passwordEncoder.encode(CORRECT_PASSWORD))
                .firstName("Test")
                .lastName("Lockout")
                .isEnabled(true)
                .failedLoginAttempts(0)
                .roles(Set.of(userRole))
                .build();
        userRepository.save(user);
    }

    @Test
    @DisplayName("Should lock account for 15 minutes after 5 consecutive failed login attempts and reject 6th attempt with correct password")
    void shouldLockAccountAfterFiveFailedAttemptsAndRejectSixthAttempt() {
        // Attempt 1 to 5 with wrong password -> all throw "Invalid username or password"
        for (int i = 1; i <= 5; i++) {
            final int attempt = i;
            AuthException ex = assertThrows(AuthException.class, () -> {
                authService.login(new LoginRequest(USERNAME, WRONG_PASSWORD));
            });
            assertEquals("Invalid username or password", ex.getMessage());

            User currentUser = userRepository.findByUsername(USERNAME).orElseThrow();
            assertEquals(attempt, currentUser.getFailedLoginAttempts(),
                    "Failed login counter should be persisted in database after attempt " + attempt);
            if (attempt < 5) {
                assertNull(currentUser.getLockedUntil(), "Account should not be locked before 5 attempts");
            } else {
                assertNotNull(currentUser.getLockedUntil(), "Account should be locked at 5th failed attempt");
            }
        }

        // 6th attempt with CORRECT password -> must be rejected as locked
        AuthException lockoutException = assertThrows(AuthException.class, () -> {
            authService.login(new LoginRequest(USERNAME, CORRECT_PASSWORD));
        });

        assertTrue(lockoutException.getMessage().contains("Account is temporarily locked due to multiple failed login attempts"),
                "Expected account lockout message, got: " + lockoutException.getMessage());
    }
}

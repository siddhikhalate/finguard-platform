package com.finguard.auth.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class JwtTokenProviderTest {

    private JwtTokenProvider jwtTokenProvider;
    private final String secret = "404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970";
    private final long expirationMs = 3600000; // 1 hour

    @BeforeEach
    void setUp() {
        jwtTokenProvider = new JwtTokenProvider(secret, expirationMs);
    }

    @Test
    @DisplayName("Should successfully generate and validate a valid JWT access token")
    void shouldGenerateAndValidateToken() {
        UUID userId = UUID.randomUUID();
        UserPrincipal principal = UserPrincipal.builder()
                .id(userId)
                .username("analyst_jane")
                .email("jane@finguard.internal")
                .authorities(List.of(new SimpleGrantedAuthority("ROLE_ANALYST")))
                .isEnabled(true)
                .build();

        String token = jwtTokenProvider.generateAccessToken(principal);

        assertNotNull(token);
        assertTrue(jwtTokenProvider.validateToken(token));
        assertEquals(userId.toString(), jwtTokenProvider.getUserIdFromToken(token));
        assertEquals("analyst_jane", jwtTokenProvider.getUsernameFromToken(token));
        assertTrue(jwtTokenProvider.getRolesFromToken(token).contains("ROLE_ANALYST"));
    }

    @Test
    @DisplayName("Should reject malformed or tampered JWT token")
    void shouldRejectMalformedToken() {
        String invalidToken = "eyJhbGciOiJIUzI1NiJ9.invalidPayload.signature";
        assertFalse(jwtTokenProvider.validateToken(invalidToken));
    }
}

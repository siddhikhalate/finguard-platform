package com.finguard.gateway.util;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class JwtUtilsTest {

    private JwtUtils jwtUtils;
    private final String secret = "404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970";
    private SecretKey key;

    @BeforeEach
    void setUp() {
        jwtUtils = new JwtUtils(secret);
        key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    @Test
    @DisplayName("Should successfully validate token and extract claims")
    void shouldValidateAndExtractClaims() {
        UUID userId = UUID.randomUUID();
        String token = Jwts.builder()
                .subject(userId.toString())
                .claim("username", "gateway_tester")
                .claim("roles", List.of("ROLE_ADMIN", "ROLE_ANALYST"))
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + 600000))
                .signWith(key, Jwts.SIG.HS256)
                .compact();

        assertTrue(jwtUtils.validateToken(token));
        assertEquals(userId.toString(), jwtUtils.getUserId(token));
        assertEquals("gateway_tester", jwtUtils.getUsername(token));
        List<String> roles = jwtUtils.getRoles(token);
        assertEquals(2, roles.size());
        assertTrue(roles.contains("ROLE_ADMIN"));
    }

    @Test
    @DisplayName("Should fail validation for tampered token")
    void shouldRejectTamperedToken() {
        String invalidToken = "tampered.jwt.payload";
        assertFalse(jwtUtils.validateToken(invalidToken));
    }
}

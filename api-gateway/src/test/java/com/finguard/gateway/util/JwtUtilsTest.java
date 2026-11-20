package com.finguard.gateway.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertThrows;

class JwtUtilsTest {

    @Test
    @DisplayName("Should throw IllegalArgumentException during startup if secret is missing or shorter than 32 bytes")
    void shouldFailStartupIfSecretTooShortOrMissing() {
        assertThrows(IllegalArgumentException.class, () -> new JwtUtils(null));
        assertThrows(IllegalArgumentException.class, () -> new JwtUtils(""));
        assertThrows(IllegalArgumentException.class, () -> new JwtUtils("short-secret-under-32-bytes"));
    }
}

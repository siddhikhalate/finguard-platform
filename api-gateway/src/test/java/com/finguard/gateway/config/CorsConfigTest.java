package com.finguard.gateway.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.cors.reactive.CorsWebFilter;

import static org.junit.jupiter.api.Assertions.assertNotNull;

class CorsConfigTest {

    @Test
    @DisplayName("Should create reactive CorsWebFilter with environment-supplied origin patterns")
    void shouldCreateCorsWebFilter() {
        CorsConfig corsConfig = new CorsConfig();
        ReflectionTestUtils.setField(corsConfig, "allowedOrigins",
                "http://localhost:3000,https://*.vercel.app,https://finguard-platform.vercel.app");

        CorsWebFilter filter = corsConfig.corsWebFilter();
        assertNotNull(filter);
    }
}

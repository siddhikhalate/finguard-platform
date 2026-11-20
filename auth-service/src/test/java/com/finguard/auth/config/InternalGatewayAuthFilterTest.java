package com.finguard.auth.config;

import com.finguard.common.constant.TracingConstants;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class InternalGatewayAuthFilterTest {

    private static final String CONFIGURED_SECRET = "test-internal-gateway-secret-12345";
    private InternalGatewayAuthFilter filter;
    private FilterChain filterChain;

    @BeforeEach
    void setUp() {
        filter = new InternalGatewayAuthFilter(CONFIGURED_SECRET);
        filterChain = mock(FilterChain.class);
    }

    @Test
    @DisplayName("Should permit request when valid X-Internal-Gateway-Key header is present")
    void shouldPermitWhenValidKeyProvided() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/auth/users/me");
        request.addHeader(TracingConstants.INTERNAL_GATEWAY_KEY_HEADER, CONFIGURED_SECRET);
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilterInternal(request, response, filterChain);

        verify(filterChain, times(1)).doFilter(request, response);
        assertEquals(200, response.getStatus());
    }

    @Test
    @DisplayName("Should block with 403 Forbidden when X-Internal-Gateway-Key header is missing")
    void shouldBlockWhenKeyMissing() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/v1/auth/login");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilterInternal(request, response, filterChain);

        verify(filterChain, never()).doFilter(request, response);
        assertEquals(403, response.getStatus());
        assertTrue(response.getContentAsString().contains("Direct microservice invocation blocked"));
    }

    @Test
    @DisplayName("Should block with 403 Forbidden when X-Internal-Gateway-Key is invalid or tampered")
    void shouldBlockWhenKeyInvalid() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/auth/users/me");
        request.addHeader(TracingConstants.INTERNAL_GATEWAY_KEY_HEADER, "wrong-forged-secret");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilterInternal(request, response, filterChain);

        verify(filterChain, never()).doFilter(request, response);
        assertEquals(403, response.getStatus());
        assertTrue(response.getContentAsString().contains("Direct microservice invocation blocked"));
    }

    @Test
    @DisplayName("Should permit exempt paths like actuator health without requiring gateway key")
    void shouldPermitExemptPathsWithoutKey() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/actuator/health");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilterInternal(request, response, filterChain);

        verify(filterChain, times(1)).doFilter(request, response);
        assertEquals(200, response.getStatus());
    }
}

package com.finguard.transaction.filter;

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
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/transactions");
        request.addHeader(TracingConstants.INTERNAL_GATEWAY_KEY_HEADER, CONFIGURED_SECRET);
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilterInternal(request, response, filterChain);

        verify(filterChain, times(1)).doFilter(request, response);
        assertEquals(200, response.getStatus());
    }

    @Test
    @DisplayName("Should block direct invocation with 403 Forbidden when key is missing")
    void shouldBlockWhenKeyMissing() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/v1/transactions");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilterInternal(request, response, filterChain);

        verify(filterChain, never()).doFilter(request, response);
        assertEquals(403, response.getStatus());
        assertTrue(response.getContentAsString().contains("Direct microservice invocation blocked"));
    }

    @Test
    @DisplayName("Should block direct invocation with 403 Forbidden when key is forged")
    void shouldBlockWhenKeyInvalid() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/transactions/accounts");
        request.addHeader(TracingConstants.INTERNAL_GATEWAY_KEY_HEADER, "forged-secret-key");
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

package com.finguard.gateway.filter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.finguard.common.constant.TracingConstants;
import com.finguard.gateway.util.JwtUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class JwtAuthenticationFilterTest {

    @Mock
    private JwtUtils jwtUtils;

    @Mock
    private GatewayFilterChain chain;

    private JwtAuthenticationFilter filter;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final String internalKey = "test-internal-gateway-key";

    @BeforeEach
    void setUp() {
        filter = new JwtAuthenticationFilter(jwtUtils, objectMapper, internalKey);
    }

    @Test
    @DisplayName("Should return 401 when Authorization header is missing")
    void shouldReturn401WhenAuthHeaderMissing() {
        MockServerHttpRequest request = MockServerHttpRequest.get("/api/v1/transactions").build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);

        Mono<Void> result = filter.filter(exchange, chain);
        result.block();

        assertEquals(HttpStatus.UNAUTHORIZED, exchange.getResponse().getStatusCode());
        verify(chain, never()).filter(any());
    }

    @Test
    @DisplayName("Should return 401 when JWT token is invalid or tampered")
    void shouldReturn401WhenTokenTampered() {
        MockServerHttpRequest request = MockServerHttpRequest.get("/api/v1/transactions")
                .header(HttpHeaders.AUTHORIZATION, "Bearer invalid.tampered.token")
                .build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);

        when(jwtUtils.validateToken("invalid.tampered.token")).thenReturn(false);

        Mono<Void> result = filter.filter(exchange, chain);
        result.block();

        assertEquals(HttpStatus.UNAUTHORIZED, exchange.getResponse().getStatusCode());
        verify(chain, never()).filter(any());
    }

    @Test
    @DisplayName("Should return 403 Forbidden when USER role attempts to access incidents workbench")
    void shouldReturn403WhenUserAccessesIncidents() {
        MockServerHttpRequest request = MockServerHttpRequest.get("/api/v1/incidents")
                .header(HttpHeaders.AUTHORIZATION, "Bearer valid.user.token")
                .build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);

        when(jwtUtils.validateToken("valid.user.token")).thenReturn(true);
        when(jwtUtils.getUserId("valid.user.token")).thenReturn(UUID.randomUUID().toString());
        when(jwtUtils.getUsername("valid.user.token")).thenReturn("regular_user");
        when(jwtUtils.getRoles("valid.user.token")).thenReturn(List.of("ROLE_USER"));

        Mono<Void> result = filter.filter(exchange, chain);
        result.block();

        assertEquals(HttpStatus.FORBIDDEN, exchange.getResponse().getStatusCode());
        verify(chain, never()).filter(any());
    }

    @Test
    @DisplayName("Should return 403 Forbidden when USER role attempts to update fraud rules")
    void shouldReturn403WhenUserUpdatesFraudRules() {
        MockServerHttpRequest request = MockServerHttpRequest.method(HttpMethod.PUT, "/api/v1/fraud/rules/rule-01")
                .header(HttpHeaders.AUTHORIZATION, "Bearer valid.user.token")
                .build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);

        when(jwtUtils.validateToken("valid.user.token")).thenReturn(true);
        when(jwtUtils.getUserId("valid.user.token")).thenReturn(UUID.randomUUID().toString());
        when(jwtUtils.getUsername("valid.user.token")).thenReturn("regular_user");
        when(jwtUtils.getRoles("valid.user.token")).thenReturn(List.of("ROLE_USER"));

        Mono<Void> result = filter.filter(exchange, chain);
        result.block();

        assertEquals(HttpStatus.FORBIDDEN, exchange.getResponse().getStatusCode());
        verify(chain, never()).filter(any());
    }

    @Test
    @DisplayName("Should return 403 Forbidden when ANALYST attempts to update user roles")
    void shouldReturn403WhenAnalystUpdatesUserRoles() {
        MockServerHttpRequest request = MockServerHttpRequest.method(HttpMethod.PUT, "/api/v1/auth/users/some-id/roles")
                .header(HttpHeaders.AUTHORIZATION, "Bearer valid.analyst.token")
                .build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);

        when(jwtUtils.validateToken("valid.analyst.token")).thenReturn(true);
        when(jwtUtils.getUserId("valid.analyst.token")).thenReturn(UUID.randomUUID().toString());
        when(jwtUtils.getUsername("valid.analyst.token")).thenReturn("analyst_jane");
        when(jwtUtils.getRoles("valid.analyst.token")).thenReturn(List.of("ROLE_ANALYST"));

        Mono<Void> result = filter.filter(exchange, chain);
        result.block();

        assertEquals(HttpStatus.FORBIDDEN, exchange.getResponse().getStatusCode());
        verify(chain, never()).filter(any());
    }

    @Test
    @DisplayName("Should pass and strip client spoofed headers when ANALYST accesses incidents")
    void shouldPassAndStripSpoofedHeadersForAnalyst() {
        MockServerHttpRequest request = MockServerHttpRequest.get("/api/v1/incidents")
                .header(HttpHeaders.AUTHORIZATION, "Bearer valid.analyst.token")
                .header(TracingConstants.USER_ROLES_HEADER, "ROLE_ADMIN") // Spoofed by client
                .header(TracingConstants.INTERNAL_GATEWAY_KEY_HEADER, "fake-client-key")
                .build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);

        String realUserId = UUID.randomUUID().toString();
        when(jwtUtils.validateToken("valid.analyst.token")).thenReturn(true);
        when(jwtUtils.getUserId("valid.analyst.token")).thenReturn(realUserId);
        when(jwtUtils.getUsername("valid.analyst.token")).thenReturn("analyst_jane");
        when(jwtUtils.getRoles("valid.analyst.token")).thenReturn(List.of("ROLE_ANALYST"));
        when(chain.filter(any())).thenReturn(Mono.empty());

        Mono<Void> result = filter.filter(exchange, chain);
        result.block();

        ArgumentCaptor<ServerWebExchange> captor = ArgumentCaptor.forClass(ServerWebExchange.class);
        verify(chain).filter(captor.capture());

        ServerWebExchange captured = captor.getValue();
        // Spoofed role should be replaced with real token roles
        assertEquals("ROLE_ANALYST", captured.getRequest().getHeaders().getFirst(TracingConstants.USER_ROLES_HEADER));
        assertEquals(realUserId, captured.getRequest().getHeaders().getFirst(TracingConstants.USER_ID_HEADER));
        assertEquals(internalKey, captured.getRequest().getHeaders().getFirst(TracingConstants.INTERNAL_GATEWAY_KEY_HEADER));
    }
}

package com.finguard.gateway.filter;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.finguard.common.constant.TracingConstants;
import com.finguard.common.dto.ErrorResponse;
import com.finguard.gateway.util.JwtUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import java.util.function.Consumer;

@Slf4j
@Component
public class JwtAuthenticationFilter implements GlobalFilter, Ordered {

    private final JwtUtils jwtUtils;
    private final ObjectMapper objectMapper;
    private final String internalGatewayKey;

    public JwtAuthenticationFilter(
            JwtUtils jwtUtils,
            ObjectMapper objectMapper,
            @Value("${finguard.security.internal-key:finguard-internal-secret-gateway-key-change-in-prod}") String internalGatewayKey) {
        this.jwtUtils = jwtUtils;
        this.objectMapper = objectMapper;
        this.internalGatewayKey = internalGatewayKey;
    }

    private static final List<String> OPEN_ENDPOINTS = List.of(
            "/api/v1/auth/login",
            "/api/v1/auth/register",
            "/api/v1/auth/refresh",
            "/actuator"
    );

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        String path = request.getURI().getPath();
        HttpMethod method = request.getMethod();

        // 1. Security Invariant: Strip all spoofable identity headers and internal secret sent by client
        Consumer<HttpHeaders> stripUntrustedHeaders = headers -> {
            headers.remove(TracingConstants.USER_ID_HEADER);
            headers.remove(TracingConstants.USERNAME_HEADER);
            headers.remove(TracingConstants.USER_ROLES_HEADER);
            headers.remove(TracingConstants.INTERNAL_GATEWAY_KEY_HEADER);
        };

        // 2. Open / Public Endpoints bypass authentication
        if (isOpenEndpoint(path)) {
            ServerHttpRequest publicMutatedRequest = request.mutate()
                    .headers(stripUntrustedHeaders)
                    .header(TracingConstants.INTERNAL_GATEWAY_KEY_HEADER, internalGatewayKey)
                    .build();
            return chain.filter(exchange.mutate().request(publicMutatedRequest).build());
        }

        // 3. Extract and Validate Bearer JWT Token
        String authHeader = request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        if (!StringUtils.hasText(authHeader) || !authHeader.startsWith("Bearer ")) {
            return onError(exchange, "Missing or malformed Authorization header", HttpStatus.UNAUTHORIZED);
        }

        String token = authHeader.substring(7);
        if (!jwtUtils.validateToken(token)) {
            return onError(exchange, "Invalid, expired, or tampered JWT token", HttpStatus.UNAUTHORIZED);
        }

        try {
            String userId = jwtUtils.getUserId(token);
            String username = jwtUtils.getUsername(token);
            List<String> roles = jwtUtils.getRoles(token);
            String rolesHeader = roles != null ? String.join(",", roles) : "";

            // 4. Enforce Role Matrix Authorization at Gateway Perimeter
            if (!isAuthorized(method, path, roles)) {
                log.warn("Access Denied: User '{}' with roles {} attempted {} {}", username, roles, method, path);
                return onError(exchange, "Access denied: insufficient permissions for this resource", HttpStatus.FORBIDDEN);
            }

            // 5. Forward trusted identity claims and internal gateway signature downstream
            ServerHttpRequest mutatedRequest = request.mutate()
                    .headers(stripUntrustedHeaders)
                    .header(TracingConstants.USER_ID_HEADER, userId)
                    .header(TracingConstants.USERNAME_HEADER, username)
                    .header(TracingConstants.USER_ROLES_HEADER, rolesHeader)
                    .header(TracingConstants.INTERNAL_GATEWAY_KEY_HEADER, internalGatewayKey)
                    .build();

            return chain.filter(exchange.mutate().request(mutatedRequest).build());

        } catch (Exception ex) {
            log.error("Failed to parse JWT token claims: {}", ex.getMessage());
            return onError(exchange, "Unable to extract user identity claims from token", HttpStatus.UNAUTHORIZED);
        }
    }

    private boolean isOpenEndpoint(String path) {
        return OPEN_ENDPOINTS.stream().anyMatch(path::startsWith);
    }

    /**
     * Enforces the FinGuard Role Matrix:
     * - USER: submit and view own transactions, view own profile
     * - ANALYST: view/assign/update incidents, view fraud assessments & AI explanations, view rules
     * - ADMIN: full access, plus fraud rule modifications and user management
     */
    private boolean isAuthorized(HttpMethod method, String path, List<String> roles) {
        if (roles == null || roles.isEmpty()) {
            return false;
        }

        // Admin has full access to all endpoints
        if (hasRole(roles, "ROLE_ADMIN")) {
            return true;
        }

        // 1. Admin-Only Endpoints
        if (path.startsWith("/api/v1/auth/users/") && path.endsWith("/roles")) {
            return false; // Non-admin already checked above
        }
        if (path.startsWith("/api/v1/fraud/rules") && (HttpMethod.PUT.equals(method) || HttpMethod.POST.equals(method) || HttpMethod.DELETE.equals(method))) {
            return false; // Modifying rules is strictly ADMIN only
        }

        // 2. Analyst & Admin Endpoints
        if (path.startsWith("/api/v1/incidents") || 
            path.startsWith("/api/v1/fraud/assessments") || 
            path.startsWith("/api/v1/ai") ||
            (path.startsWith("/api/v1/fraud/rules") && HttpMethod.GET.equals(method))) {
            return hasRole(roles, "ROLE_ANALYST");
        }

        // 3. User, Analyst & Admin Endpoints (Transactions & Self Profile)
        if (path.startsWith("/api/v1/transactions") || path.equals("/api/v1/auth/me")) {
            return hasRole(roles, "ROLE_USER") || hasRole(roles, "ROLE_ANALYST");
        }

        // Default: If authenticated but route not explicitly matching, allow USER/ANALYST/ADMIN
        return true;
    }

    private boolean hasRole(List<String> roles, String requiredRole) {
        return roles.stream().anyMatch(r -> {
            String normalized = r.startsWith("ROLE_") ? r : "ROLE_" + r;
            return normalized.equalsIgnoreCase(requiredRole);
        });
    }

    private Mono<Void> onError(ServerWebExchange exchange, String message, HttpStatus status) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(status);
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);

        String correlationId = exchange.getRequest().getHeaders().getFirst(TracingConstants.CORRELATION_ID_HEADER);
        String path = exchange.getRequest().getURI().getPath();

        ErrorResponse errorResponse = ErrorResponse.builder()
                .timestamp(Instant.now())
                .status(status.value())
                .error(status.getReasonPhrase())
                .message(message)
                .path(path)
                .correlationId(correlationId)
                .build();

        byte[] bytes;
        try {
            bytes = objectMapper.writeValueAsBytes(errorResponse);
        } catch (JsonProcessingException e) {
            bytes = ("{\"status\":" + status.value() + ",\"error\":\"" + status.getReasonPhrase() + "\",\"message\":\"" + message + "\"}").getBytes(StandardCharsets.UTF_8);
        }

        DataBuffer buffer = response.bufferFactory().wrap(bytes);
        return response.writeWith(Mono.just(buffer));
    }

    @Override
    public int getOrder() {
        return -90; // Runs immediately after CorrelationIdGlobalFilter (-100)
    }
}

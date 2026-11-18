package com.finguard.gateway.filter;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.finguard.common.constant.TracingConstants;
import com.finguard.common.dto.ErrorResponse;
import com.finguard.gateway.util.JwtUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpHeaders;
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

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter implements GlobalFilter, Ordered {

    private final JwtUtils jwtUtils;
    private final ObjectMapper objectMapper;

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

        // Check if the current request path matches any open/public endpoints
        if (isOpenEndpoint(path)) {
            return chain.filter(exchange);
        }

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

            // Propagate security claims to downstream microservices via trusted headers
            ServerHttpRequest mutatedRequest = request.mutate()
                    .header(TracingConstants.USER_ID_HEADER, userId)
                    .header(TracingConstants.USERNAME_HEADER, username)
                    .header(TracingConstants.USER_ROLES_HEADER, rolesHeader)
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
            bytes = ("{\"error\":\"" + message + "\"}").getBytes(StandardCharsets.UTF_8);
        }

        DataBuffer buffer = response.bufferFactory().wrap(bytes);
        return response.writeWith(Mono.just(buffer));
    }

    @Override
    public int getOrder() {
        return -1; // Executed immediately following CorrelationIdGlobalFilter
    }
}

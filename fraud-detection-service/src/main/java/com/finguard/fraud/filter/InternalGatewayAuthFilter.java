package com.finguard.fraud.filter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.finguard.common.constant.TracingConstants;
import com.finguard.common.dto.ErrorResponse;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;

/**
 * Perimeter defense filter: rejects direct invocations bypassing the API Gateway.
 * Compares the X-Internal-Gateway-Key header in constant time to prevent side-channel timing attacks (CWE-208).
 */
@Slf4j
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class InternalGatewayAuthFilter extends OncePerRequestFilter {

    @Value("${finguard.security.internal-key:finguard-internal-secret-gateway-key-change-in-prod}")
    private String expectedInternalKey;

    private final ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(com.fasterxml.jackson.databind.SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    public InternalGatewayAuthFilter() {}

    public InternalGatewayAuthFilter(String expectedInternalKey) {
        this.expectedInternalKey = expectedInternalKey;
    }

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain) throws ServletException, IOException {

        String path = request.getRequestURI();

        // Exempt health checks and documentation endpoints
        if (isExemptPath(path)) {
            filterChain.doFilter(request, response);
            return;
        }

        String providedKey = request.getHeader(TracingConstants.INTERNAL_GATEWAY_KEY_HEADER);

        if (!isValidKey(providedKey)) {
            log.warn("Blocked direct access attempt lacking valid gateway key: path={}, remoteAddr={}",
                    path, request.getRemoteAddr());
            sendForbiddenResponse(request, response, "Direct microservice invocation blocked: valid gateway secret required");
            return;
        }

        filterChain.doFilter(request, response);
    }

    private boolean isExemptPath(String path) {
        return path.startsWith("/actuator")
                || path.startsWith("/swagger-ui")
                || path.startsWith("/v3/api-docs")
                || path.startsWith("/api-docs");
    }

    private boolean isValidKey(String providedKey) {
        if (!StringUtils.hasText(providedKey) || !StringUtils.hasText(expectedInternalKey)) {
            return false;
        }
        return MessageDigest.isEqual(
                providedKey.getBytes(StandardCharsets.UTF_8),
                expectedInternalKey.getBytes(StandardCharsets.UTF_8)
        );
    }

    private void sendForbiddenResponse(HttpServletRequest request, HttpServletResponse response, String message) throws IOException {
        response.setStatus(HttpStatus.FORBIDDEN.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");

        ErrorResponse errorResponse = ErrorResponse.builder()
                .timestamp(Instant.now())
                .status(HttpStatus.FORBIDDEN.value())
                .error("Forbidden")
                .message(message)
                .path(request.getRequestURI())
                .correlationId(request.getHeader(TracingConstants.CORRELATION_ID_HEADER))
                .build();

        response.getWriter().write(objectMapper.writeValueAsString(errorResponse));
    }
}

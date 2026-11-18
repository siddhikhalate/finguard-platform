package com.finguard.common.constant;

/**
 * Standard HTTP and messaging headers for distributed tracing and authorization context propagation.
 */
public final class TracingConstants {

    private TracingConstants() {
        // Prevent instantiation
    }

    public static final String CORRELATION_ID_HEADER = "X-Correlation-Id";
    public static final String USER_ID_HEADER = "X-User-Id";
    public static final String USERNAME_HEADER = "X-Username";
    public static final String USER_ROLES_HEADER = "X-User-Roles";
    public static final String IDEMPOTENCY_KEY_HEADER = "Idempotency-Key";
}

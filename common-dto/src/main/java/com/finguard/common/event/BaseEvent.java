package com.finguard.common.event;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.io.Serializable;
import java.time.Instant;
import java.util.UUID;

/**
 * Base event containing standard metadata envelopes for Kafka event streaming.
 */
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public abstract class BaseEvent implements Serializable {

    @lombok.Builder.Default
    private String eventId = UUID.randomUUID().toString();

    private String eventType;

    private String correlationId;

    @lombok.Builder.Default
    private Instant timestamp = Instant.now();

    @lombok.Builder.Default
    private String version = "1.0";
}

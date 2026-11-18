package com.finguard.incident.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuditLogDto {

    private UUID id;
    private String actorUsername;
    private String action;
    private String comment;
    private String previousStatus;
    private String newStatus;
    private Instant createdAt;
}

package com.finguard.incident.dto;

import com.finguard.common.enums.IncidentPriority;
import com.finguard.common.enums.IncidentStatus;
import com.finguard.common.enums.RiskLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IncidentDetailDto {

    private UUID id;
    private String incidentReference;
    private UUID transactionId;
    private UUID accountId;
    private BigDecimal amount;
    private String currency;
    private String location;
    private int riskScore;
    private RiskLevel riskLevel;
    private IncidentStatus status;
    private IncidentPriority priority;
    private UUID assignedTo;
    private String assignedToUsername;
    private String resolutionNotes;
    private AiExplanationDto aiExplanation;
    private List<AuditLogDto> auditLogs;
    private Instant createdAt;
    private Instant updatedAt;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AiExplanationDto {
        private String summary;
        private String factors;
        private String recommendedActions;
        private String confidenceLevel;
        private String modelUsed;
        private boolean isFallback;
    }
}

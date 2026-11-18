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
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IncidentSummaryDto {

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
    private Instant createdAt;
    private Instant updatedAt;
}

package com.finguard.common.event;

import com.finguard.common.dto.TriggeredRuleDto;
import com.finguard.common.enums.IncidentPriority;
import com.finguard.common.enums.IncidentStatus;
import com.finguard.common.enums.RiskLevel;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * Event published when a high-risk or critical transaction triggers the creation of an incident.
 * Handled by the AI Explanation Service to prepare natural language analysis.
 */
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class FraudIncidentCreatedEvent extends BaseEvent {

    private UUID incidentId;
    private String incidentReference;
    private UUID transactionId;
    private UUID accountId;
    private BigDecimal amount;
    private String currency;
    private String location;
    private int riskScore;
    private RiskLevel riskLevel;
    private IncidentPriority priority;
    private IncidentStatus status;
    private List<TriggeredRuleDto> triggeredRules;
}

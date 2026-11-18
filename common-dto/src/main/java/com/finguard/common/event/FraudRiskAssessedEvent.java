package com.finguard.common.event;

import com.finguard.common.dto.TriggeredRuleDto;
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
 * Event published when the Fraud Detection Engine completes evaluating a transaction.
 * Handled by the Incident Management Service.
 */
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class FraudRiskAssessedEvent extends BaseEvent {

    private UUID assessmentId;
    private UUID transactionId;
    private UUID accountId;
    private BigDecimal amount;
    private String currency;
    private String location;
    private int riskScore;
    private RiskLevel riskLevel;
    private String actionRecommended;
    private List<TriggeredRuleDto> triggeredRules;
}

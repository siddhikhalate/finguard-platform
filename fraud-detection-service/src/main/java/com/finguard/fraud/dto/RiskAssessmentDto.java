package com.finguard.fraud.dto;

import com.finguard.common.dto.TriggeredRuleDto;
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
public class RiskAssessmentDto {

    private UUID id;
    private UUID transactionId;
    private UUID accountId;
    private BigDecimal amount;
    private String currency;
    private int riskScore;
    private RiskLevel riskLevel;
    private String actionRecommended;
    private List<TriggeredRuleDto> triggeredRules;
    private Instant evaluationTimestamp;
}

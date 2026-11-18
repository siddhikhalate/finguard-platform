package com.finguard.ai.dto;

import com.finguard.common.dto.TriggeredRuleDto;
import com.finguard.common.enums.RiskLevel;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AiExplanationRequest {

    private UUID incidentId;
    private UUID transactionId;
    private UUID accountId;

    @NotNull(message = "Amount is required")
    private BigDecimal amount;

    @Builder.Default
    private String currency = "USD";

    private String location;

    @NotNull(message = "Risk score is required")
    private Integer riskScore;

    @NotNull(message = "Risk level is required")
    private RiskLevel riskLevel;

    private List<TriggeredRuleDto> triggeredRules;
}

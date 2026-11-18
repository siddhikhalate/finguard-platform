package com.finguard.fraud.rule;

import com.finguard.common.dto.TriggeredRuleDto;
import com.finguard.common.enums.RiskLevel;
import com.finguard.common.enums.RuleType;
import com.finguard.common.event.TransactionCreatedEvent;
import com.finguard.fraud.entity.FraudRuleEntity;
import com.finguard.fraud.repository.FraudRuleRepository;
import lombok.Builder;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Component
public class FraudRuleEngine {

    private final FraudRuleRepository ruleRepository;
    private final Map<RuleType, FraudRuleStrategy> strategies;

    public FraudRuleEngine(FraudRuleRepository ruleRepository, List<FraudRuleStrategy> strategyList) {
        this.ruleRepository = ruleRepository;
        this.strategies = strategyList.stream()
                .collect(Collectors.toMap(FraudRuleStrategy::getSupportedType, Function.identity()));
    }

    public RiskEvaluationResult evaluate(TransactionCreatedEvent event) {
        List<FraudRuleEntity> activeRules = ruleRepository.findByIsActiveTrue();
        List<TriggeredRuleDto> triggeredRules = new ArrayList<>();
        int totalScore = 0;

        for (FraudRuleEntity ruleConfig : activeRules) {
            FraudRuleStrategy strategy = strategies.get(ruleConfig.getRuleType());
            if (strategy != null) {
                Optional<TriggeredRuleDto> trigger = strategy.evaluate(event, ruleConfig);
                if (trigger.isPresent()) {
                    triggeredRules.add(trigger.get());
                    totalScore += trigger.get().getPointsAdded();
                    log.info("Rule [{}] triggered for transaction {}: +{} points. Reason: {}",
                            ruleConfig.getRuleCode(), event.getTransactionId(),
                            trigger.get().getPointsAdded(), trigger.get().getTriggerReason());
                }
            }
        }

        // Clamp score between 0 and 100
        int finalScore = Math.min(100, Math.max(0, totalScore));
        RiskLevel riskLevel = RiskLevel.fromScore(finalScore);

        String actionRecommended;
        switch (riskLevel) {
            case CRITICAL -> actionRecommended = "BLOCK_AND_FREEZE_ACCOUNT";
            case HIGH -> actionRecommended = "ESCALATE_TO_ANALYST_QUEUE";
            case MEDIUM -> actionRecommended = "FLAG_FOR_MONITORING";
            default -> actionRecommended = "APPROVE";
        }

        return RiskEvaluationResult.builder()
                .riskScore(finalScore)
                .riskLevel(riskLevel)
                .actionRecommended(actionRecommended)
                .triggeredRules(triggeredRules)
                .build();
    }

    @Data
    @Builder
    public static class RiskEvaluationResult {
        private int riskScore;
        private RiskLevel riskLevel;
        private String actionRecommended;
        private List<TriggeredRuleDto> triggeredRules;
    }
}

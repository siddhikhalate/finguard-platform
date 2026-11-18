package com.finguard.fraud.rule;

import com.finguard.common.dto.TriggeredRuleDto;
import com.finguard.common.enums.RuleType;
import com.finguard.common.event.TransactionCreatedEvent;
import com.finguard.fraud.entity.FraudRuleEntity;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class HighAmountRuleStrategy implements FraudRuleStrategy {

    @Override
    public RuleType getSupportedType() {
        return RuleType.AMOUNT_THRESHOLD;
    }

    @Override
    public Optional<TriggeredRuleDto> evaluate(TransactionCreatedEvent event, FraudRuleEntity ruleConfig) {
        if (event.getAmount() != null && event.getAmount().compareTo(ruleConfig.getThresholdValue()) > 0) {
            return Optional.of(TriggeredRuleDto.builder()
                    .ruleCode(ruleConfig.getRuleCode())
                    .ruleName(ruleConfig.getRuleName())
                    .pointsAdded(ruleConfig.getWeight())
                    .triggerReason(String.format("Transaction amount $%s exceeds threshold of $%s",
                            event.getAmount(), ruleConfig.getThresholdValue()))
                    .build());
        }
        return Optional.empty();
    }
}

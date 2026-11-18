package com.finguard.fraud.rule;

import com.finguard.common.dto.TriggeredRuleDto;
import com.finguard.common.enums.RuleType;
import com.finguard.common.event.TransactionCreatedEvent;
import com.finguard.fraud.entity.FraudRuleEntity;
import com.finguard.fraud.redis.VelocityTracker;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@RequiredArgsConstructor
public class RapidActivityRuleStrategy implements FraudRuleStrategy {

    private final VelocityTracker velocityTracker;

    @Override
    public RuleType getSupportedType() {
        return RuleType.RAPID_ACTIVITY;
    }

    @Override
    public Optional<TriggeredRuleDto> evaluate(TransactionCreatedEvent event, FraudRuleEntity ruleConfig) {
        int windowSeconds = ruleConfig.getTimeWindowSeconds() > 0 ? ruleConfig.getTimeWindowSeconds() : 120;
        long recentCount = velocityTracker.recordAndGetTransactionCount(
                event.getAccountId(), event.getTransactionId(), windowSeconds);

        if (recentCount > ruleConfig.getThresholdValue().longValue()) {
            return Optional.of(TriggeredRuleDto.builder()
                    .ruleCode(ruleConfig.getRuleCode())
                    .ruleName(ruleConfig.getRuleName())
                    .pointsAdded(ruleConfig.getWeight())
                    .triggerReason(String.format("High transaction burst: %d transactions recorded within %d seconds (limit: %s)",
                            recentCount, windowSeconds, ruleConfig.getThresholdValue()))
                    .build());
        }
        return Optional.empty();
    }
}

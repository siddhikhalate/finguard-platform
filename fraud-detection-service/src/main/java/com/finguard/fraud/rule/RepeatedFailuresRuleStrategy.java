package com.finguard.fraud.rule;

import com.finguard.common.dto.TriggeredRuleDto;
import com.finguard.common.enums.RuleType;
import com.finguard.common.enums.TransactionStatus;
import com.finguard.common.event.TransactionCreatedEvent;
import com.finguard.fraud.entity.FraudRuleEntity;
import com.finguard.fraud.redis.VelocityTracker;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@RequiredArgsConstructor
public class RepeatedFailuresRuleStrategy implements FraudRuleStrategy {

    private final VelocityTracker velocityTracker;

    @Override
    public RuleType getSupportedType() {
        return RuleType.FAILED_ATTEMPTS;
    }

    @Override
    public Optional<TriggeredRuleDto> evaluate(TransactionCreatedEvent event, FraudRuleEntity ruleConfig) {
        if (event.getStatus() == TransactionStatus.FAILED) {
            int windowSeconds = ruleConfig.getTimeWindowSeconds() > 0 ? ruleConfig.getTimeWindowSeconds() : 300;
            long failureCount = velocityTracker.recordAndGetFailedAttemptCount(
                    event.getAccountId(), event.getTransactionId(), windowSeconds);

            if (failureCount >= ruleConfig.getThresholdValue().longValue()) {
                return Optional.of(TriggeredRuleDto.builder()
                        .ruleCode(ruleConfig.getRuleCode())
                        .ruleName(ruleConfig.getRuleName())
                        .pointsAdded(ruleConfig.getWeight())
                        .triggerReason(String.format("Card probing warning: %d failed transactions within %d seconds",
                                failureCount, windowSeconds))
                        .build());
            }
        }
        return Optional.empty();
    }
}

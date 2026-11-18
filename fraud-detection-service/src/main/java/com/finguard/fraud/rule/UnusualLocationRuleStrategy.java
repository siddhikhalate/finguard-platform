package com.finguard.fraud.rule;

import com.finguard.common.dto.TriggeredRuleDto;
import com.finguard.common.enums.RuleType;
import com.finguard.common.event.TransactionCreatedEvent;
import com.finguard.fraud.entity.FraudRuleEntity;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.Set;

@Component
public class UnusualLocationRuleStrategy implements FraudRuleStrategy {

    // High-risk or anomalous destination indicators in synthetic test datasets
    private static final Set<String> HIGH_RISK_LOCATIONS = Set.of(
            "LAGOS, NIGERIA", "CAYMAN ISLANDS", "UNKNOWN PROXY", "TOR EXIT NODE", "OFFSHORE HAVEN"
    );

    @Override
    public RuleType getSupportedType() {
        return RuleType.LOCATION_DEVIATION;
    }

    @Override
    public Optional<TriggeredRuleDto> evaluate(TransactionCreatedEvent event, FraudRuleEntity ruleConfig) {
        String location = event.getLocation();
        if (location != null) {
            String normalizedLocation = location.toUpperCase().trim();
            boolean isAnomalous = HIGH_RISK_LOCATIONS.stream().anyMatch(normalizedLocation::contains);

            if (isAnomalous) {
                return Optional.of(TriggeredRuleDto.builder()
                        .ruleCode(ruleConfig.getRuleCode())
                        .ruleName(ruleConfig.getRuleName())
                        .pointsAdded(ruleConfig.getWeight())
                        .triggerReason(String.format("Transaction executed from flagged or anomalous location: '%s'", location))
                        .build());
            }
        }
        return Optional.empty();
    }
}

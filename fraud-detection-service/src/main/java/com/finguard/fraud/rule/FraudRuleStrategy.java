package com.finguard.fraud.rule;

import com.finguard.common.dto.TriggeredRuleDto;
import com.finguard.common.enums.RuleType;
import com.finguard.common.event.TransactionCreatedEvent;
import com.finguard.fraud.entity.FraudRuleEntity;

import java.util.Optional;

/**
 * Strategy interface for pluggable deterministic fraud evaluation rules.
 */
public interface FraudRuleStrategy {

    RuleType getSupportedType();

    Optional<TriggeredRuleDto> evaluate(TransactionCreatedEvent event, FraudRuleEntity ruleConfig);
}

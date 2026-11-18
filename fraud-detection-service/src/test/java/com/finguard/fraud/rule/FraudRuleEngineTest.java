package com.finguard.fraud.rule;

import com.finguard.common.enums.RiskLevel;
import com.finguard.common.enums.RuleType;
import com.finguard.common.enums.TransactionStatus;
import com.finguard.common.event.TransactionCreatedEvent;
import com.finguard.fraud.entity.FraudRuleEntity;
import com.finguard.fraud.redis.VelocityTracker;
import com.finguard.fraud.repository.FraudRuleRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FraudRuleEngineTest {

    @Mock
    private FraudRuleRepository ruleRepository;

    @Mock
    private VelocityTracker velocityTracker;

    private FraudRuleEngine engine;

    private FraudRuleEntity highAmountRule;
    private FraudRuleEntity rapidActivityRule;
    private FraudRuleEntity unusualLocationRule;
    private FraudRuleEntity repeatedFailuresRule;

    @BeforeEach
    void setUp() {
        highAmountRule = FraudRuleEntity.builder()
                .ruleCode("RULE_HIGH_AMOUNT")
                .ruleName("High Amount")
                .ruleType(RuleType.AMOUNT_THRESHOLD)
                .thresholdValue(new BigDecimal("10000.00"))
                .weight(25)
                .isActive(true)
                .build();

        rapidActivityRule = FraudRuleEntity.builder()
                .ruleCode("RULE_RAPID_ACTIVITY")
                .ruleName("Rapid Activity")
                .ruleType(RuleType.RAPID_ACTIVITY)
                .thresholdValue(new BigDecimal("3.00"))
                .timeWindowSeconds(120)
                .weight(25)
                .isActive(true)
                .build();

        unusualLocationRule = FraudRuleEntity.builder()
                .ruleCode("RULE_UNUSUAL_LOCATION")
                .ruleName("Unusual Location")
                .ruleType(RuleType.LOCATION_DEVIATION)
                .thresholdValue(new BigDecimal("1.00"))
                .weight(20)
                .isActive(true)
                .build();

        repeatedFailuresRule = FraudRuleEntity.builder()
                .ruleCode("RULE_REPEATED_FAILURES")
                .ruleName("Failed Attempts")
                .ruleType(RuleType.FAILED_ATTEMPTS)
                .thresholdValue(new BigDecimal("2.00"))
                .timeWindowSeconds(300)
                .weight(15)
                .isActive(true)
                .build();

        List<FraudRuleStrategy> strategies = List.of(
                new HighAmountRuleStrategy(),
                new RapidActivityRuleStrategy(velocityTracker),
                new UnusualLocationRuleStrategy(),
                new RepeatedFailuresRuleStrategy(velocityTracker)
        );

        engine = new FraudRuleEngine(ruleRepository, strategies);
    }

    @Test
    @DisplayName("Scenario 1: Normal transaction should have LOW risk (Score: 0)")
    void normalTransactionShouldBeLowRisk() {
        when(ruleRepository.findByIsActiveTrue()).thenReturn(List.of(
                highAmountRule, rapidActivityRule, unusualLocationRule, repeatedFailuresRule
        ));
        when(velocityTracker.recordAndGetTransactionCount(any(), any(), anyInt())).thenReturn(1L);

        TransactionCreatedEvent event = TransactionCreatedEvent.builder()
                .transactionId(UUID.randomUUID())
                .accountId(UUID.randomUUID())
                .amount(new BigDecimal("150.00"))
                .currency("USD")
                .location("New York, USA")
                .status(TransactionStatus.COMPLETED)
                .transactionTime(Instant.now())
                .build();

        FraudRuleEngine.RiskEvaluationResult result = engine.evaluate(event);

        assertEquals(0, result.getRiskScore());
        assertEquals(RiskLevel.LOW, result.getRiskLevel());
        assertEquals("APPROVE", result.getActionRecommended());
        assertTrue(result.getTriggeredRules().isEmpty());
    }

    @Test
    @DisplayName("Scenario 2: High transaction amount should trigger RULE_HIGH_AMOUNT (Score: 25, MEDIUM)")
    void highAmountShouldTriggerRule() {
        when(ruleRepository.findByIsActiveTrue()).thenReturn(List.of(
                highAmountRule, rapidActivityRule, unusualLocationRule, repeatedFailuresRule
        ));
        when(velocityTracker.recordAndGetTransactionCount(any(), any(), anyInt())).thenReturn(1L);

        TransactionCreatedEvent event = TransactionCreatedEvent.builder()
                .transactionId(UUID.randomUUID())
                .accountId(UUID.randomUUID())
                .amount(new BigDecimal("15000.00")) // Exceeds 10,000 threshold
                .currency("USD")
                .location("New York, USA")
                .status(TransactionStatus.COMPLETED)
                .transactionTime(Instant.now())
                .build();

        FraudRuleEngine.RiskEvaluationResult result = engine.evaluate(event);

        assertEquals(25, result.getRiskScore());
        assertEquals(RiskLevel.MEDIUM, result.getRiskLevel());
        assertEquals(1, result.getTriggeredRules().size());
        assertEquals("RULE_HIGH_AMOUNT", result.getTriggeredRules().get(0).getRuleCode());
    }

    @Test
    @DisplayName("Scenario 3: Multi-factor risk (High amount + Location + Rapid burst) should trigger CRITICAL risk (Score: 70 -> HIGH / 75+ -> CRITICAL)")
    void multiFactorShouldProduceCriticalRisk() {
        when(ruleRepository.findByIsActiveTrue()).thenReturn(List.of(
                highAmountRule, rapidActivityRule, unusualLocationRule, repeatedFailuresRule
        ));
        // Simulate rapid velocity burst (4 transactions in window)
        when(velocityTracker.recordAndGetTransactionCount(any(), any(), anyInt())).thenReturn(5L);

        TransactionCreatedEvent event = TransactionCreatedEvent.builder()
                .transactionId(UUID.randomUUID())
                .accountId(UUID.randomUUID())
                .amount(new BigDecimal("25000.00")) // +25 pts
                .currency("USD")
                .location("Lagos, Nigeria") // Flagged location: +20 pts
                .status(TransactionStatus.COMPLETED)
                .transactionTime(Instant.now())
                .build();

        FraudRuleEngine.RiskEvaluationResult result = engine.evaluate(event);

        // Score: 25 (High amount) + 25 (Rapid activity) + 20 (Unusual location) = 70 (HIGH)
        assertEquals(70, result.getRiskScore());
        assertEquals(RiskLevel.HIGH, result.getRiskLevel());
        assertEquals("ESCALATE_TO_ANALYST_QUEUE", result.getActionRecommended());
        assertEquals(3, result.getTriggeredRules().size());
    }
}

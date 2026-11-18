package com.finguard.fraud.service;

import com.finguard.common.constant.KafkaTopics;
import com.finguard.common.dto.TriggeredRuleDto;
import com.finguard.common.enums.RiskLevel;
import com.finguard.common.enums.TransactionStatus;
import com.finguard.common.event.FraudRiskAssessedEvent;
import com.finguard.common.event.TransactionCreatedEvent;
import com.finguard.fraud.dto.RiskAssessmentDto;
import com.finguard.fraud.entity.RiskAssessment;
import com.finguard.fraud.repository.FraudRuleRepository;
import com.finguard.fraud.repository.RiskAssessmentRepository;
import com.finguard.fraud.rule.FraudRuleEngine;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FraudDetectionServiceTest {

    @Mock
    private FraudRuleEngine ruleEngine;

    @Mock
    private RiskAssessmentRepository riskAssessmentRepository;

    @Mock
    private FraudRuleRepository fraudRuleRepository;

    @Mock
    private KafkaTemplate<String, Object> kafkaTemplate;

    @InjectMocks
    private FraudDetectionService fraudDetectionService;

    private UUID transactionId;
    private UUID accountId;
    private TransactionCreatedEvent transactionEvent;

    @BeforeEach
    void setUp() {
        transactionId = UUID.randomUUID();
        accountId = UUID.randomUUID();
        transactionEvent = TransactionCreatedEvent.builder()
                .transactionId(transactionId)
                .accountId(accountId)
                .amount(new BigDecimal("12000.00"))
                .currency("USD")
                .location("Chicago, USA")
                .status(TransactionStatus.COMPLETED)
                .transactionTime(Instant.now())
                .correlationId("corr-123")
                .build();
    }

    @Test
    @DisplayName("Should assess transaction, persist assessment, and publish FraudRiskAssessedEvent")
    void shouldAssessAndPublishRiskEvent() {
        when(riskAssessmentRepository.existsByTransactionId(transactionId)).thenReturn(false);

        FraudRuleEngine.RiskEvaluationResult evalResult = FraudRuleEngine.RiskEvaluationResult.builder()
                .riskScore(25)
                .riskLevel(RiskLevel.MEDIUM)
                .actionRecommended("FLAG_FOR_MONITORING")
                .triggeredRules(List.of(TriggeredRuleDto.builder()
                        .ruleCode("RULE_HIGH_AMOUNT")
                        .ruleName("High Amount")
                        .pointsAdded(25)
                        .triggerReason("Amount exceeds threshold")
                        .build()))
                .build();

        when(ruleEngine.evaluate(transactionEvent)).thenReturn(evalResult);
        when(riskAssessmentRepository.save(any(RiskAssessment.class))).thenAnswer(inv -> {
            RiskAssessment ra = inv.getArgument(0);
            ra.setId(UUID.randomUUID());
            return ra;
        });
        when(kafkaTemplate.send(any(), any(), any())).thenReturn(new CompletableFuture<>());

        RiskAssessmentDto result = fraudDetectionService.assessTransaction(transactionEvent);

        assertNotNull(result);
        assertEquals(25, result.getRiskScore());
        assertEquals(RiskLevel.MEDIUM, result.getRiskLevel());

        // Verify Kafka event published to correct topic with accountId partition key
        ArgumentCaptor<FraudRiskAssessedEvent> eventCaptor = ArgumentCaptor.forClass(FraudRiskAssessedEvent.class);
        verify(kafkaTemplate).send(eq(KafkaTopics.FRAUD_RISK_ASSESSED), eq(accountId.toString()), eventCaptor.capture());
        FraudRiskAssessedEvent emitted = eventCaptor.getValue();
        assertEquals(25, emitted.getRiskScore());
        assertEquals(RiskLevel.MEDIUM, emitted.getRiskLevel());
        assertEquals("corr-123", emitted.getCorrelationId());
    }

    @Test
    @DisplayName("Should skip rule evaluation if transaction was already assessed (Idempotent Kafka Consumer)")
    void shouldSkipDuplicateAssessment() {
        when(riskAssessmentRepository.existsByTransactionId(transactionId)).thenReturn(true);

        RiskAssessment existing = RiskAssessment.builder()
                .id(UUID.randomUUID())
                .transactionId(transactionId)
                .accountId(accountId)
                .amount(new BigDecimal("12000.00"))
                .currency("USD")
                .riskScore(25)
                .riskLevel(RiskLevel.MEDIUM)
                .actionRecommended("FLAG_FOR_MONITORING")
                .evaluationTimestamp(Instant.now())
                .triggeredRules(List.of())
                .build();

        when(riskAssessmentRepository.findByTransactionId(transactionId)).thenReturn(Optional.of(existing));

        RiskAssessmentDto result = fraudDetectionService.assessTransaction(transactionEvent);

        assertNotNull(result);
        // Rule engine should not be called again
        verify(ruleEngine, never()).evaluate(any());
        // Kafka template should not publish duplicate event
        verify(kafkaTemplate, never()).send(any(), any(), any());
    }
}

package com.finguard.fraud.service;

import com.finguard.common.constant.KafkaTopics;
import com.finguard.common.dto.TriggeredRuleDto;
import com.finguard.common.event.FraudRiskAssessedEvent;
import com.finguard.common.event.TransactionCreatedEvent;
import com.finguard.fraud.dto.FraudRuleUpdateRequest;
import com.finguard.fraud.dto.RiskAssessmentDto;
import com.finguard.fraud.entity.FraudRuleEntity;
import com.finguard.fraud.entity.RiskAssessment;
import com.finguard.fraud.entity.TriggeredRuleEntity;
import com.finguard.fraud.repository.FraudRuleRepository;
import com.finguard.fraud.repository.RiskAssessmentRepository;
import com.finguard.fraud.rule.FraudRuleEngine;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class FraudDetectionService {

    private final FraudRuleEngine ruleEngine;
    private final RiskAssessmentRepository riskAssessmentRepository;
    private final FraudRuleRepository fraudRuleRepository;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Transactional
    public RiskAssessmentDto assessTransaction(TransactionCreatedEvent event) {
        // 1. Idempotency Check: Prevent duplicate evaluations for Kafka replay/retries
        if (riskAssessmentRepository.existsByTransactionId(event.getTransactionId())) {
            log.info("Transaction {} already evaluated. Skipping duplicate Kafka event.", event.getTransactionId());
            return getAssessmentByTransactionId(event.getTransactionId());
        }

        log.info("Evaluating transaction {} (Amount: {} {}) for fraud indicators",
                event.getTransactionId(), event.getAmount(), event.getCurrency());

        // 2. Evaluate Deterministic Fraud Rules Engine
        FraudRuleEngine.RiskEvaluationResult result = ruleEngine.evaluate(event);

        // 3. Persist Risk Assessment
        RiskAssessment assessment = RiskAssessment.builder()
                .transactionId(event.getTransactionId())
                .accountId(event.getAccountId())
                .amount(event.getAmount())
                .currency(event.getCurrency())
                .riskScore(result.getRiskScore())
                .riskLevel(result.getRiskLevel())
                .actionRecommended(result.getActionRecommended())
                .build();

        // 4. Attach Triggered Rule Child Entities
        List<TriggeredRuleEntity> ruleEntities = result.getTriggeredRules().stream()
                .map(tr -> TriggeredRuleEntity.builder()
                        .assessment(assessment)
                        .ruleCode(tr.getRuleCode())
                        .ruleName(tr.getRuleName())
                        .pointsAdded(tr.getPointsAdded())
                        .triggerReason(tr.getTriggerReason())
                        .build())
                .collect(Collectors.toList());

        assessment.setTriggeredRules(ruleEntities);
        RiskAssessment savedAssessment = riskAssessmentRepository.save(assessment);

        // 5. Emit FraudRiskAssessedEvent to Kafka
        FraudRiskAssessedEvent riskAssessedEvent = FraudRiskAssessedEvent.builder()
                .eventId(UUID.randomUUID().toString())
                .eventType("FRAUD_RISK_ASSESSED")
                .correlationId(event.getCorrelationId())
                .timestamp(Instant.now())
                .assessmentId(savedAssessment.getId())
                .transactionId(savedAssessment.getTransactionId())
                .accountId(savedAssessment.getAccountId())
                .amount(savedAssessment.getAmount())
                .currency(savedAssessment.getCurrency())
                .location(event.getLocation())
                .riskScore(savedAssessment.getRiskScore())
                .riskLevel(savedAssessment.getRiskLevel())
                .actionRecommended(savedAssessment.getActionRecommended())
                .triggeredRules(result.getTriggeredRules())
                .build();

        kafkaTemplate.send(KafkaTopics.FRAUD_RISK_ASSESSED, savedAssessment.getAccountId().toString(), riskAssessedEvent);
        log.info("Emitted FraudRiskAssessedEvent for txn {} with RiskScore: {} ({})",
                event.getTransactionId(), savedAssessment.getRiskScore(), savedAssessment.getRiskLevel());

        return mapToDto(savedAssessment);
    }

    @Transactional(readOnly = true)
    public RiskAssessmentDto getAssessmentByTransactionId(UUID transactionId) {
        RiskAssessment assessment = riskAssessmentRepository.findByTransactionId(transactionId)
                .orElseThrow(() -> new RuntimeException("Assessment not found for transaction: " + transactionId));
        return mapToDto(assessment);
    }

    @Transactional(readOnly = true)
    public Page<RiskAssessmentDto> getRecentAssessments(Pageable pageable) {
        return riskAssessmentRepository.findAllByOrderByEvaluationTimestampDesc(pageable)
                .map(this::mapToDto);
    }

    @Transactional(readOnly = true)
    public List<FraudRuleEntity> getAllRules() {
        return fraudRuleRepository.findAll();
    }

    @Transactional
    public FraudRuleEntity updateRule(UUID ruleId, FraudRuleUpdateRequest request) {
        FraudRuleEntity rule = fraudRuleRepository.findById(ruleId)
                .orElseThrow(() -> new RuntimeException("Fraud rule not found with ID: " + ruleId));

        if (request.getWeight() != null) rule.setWeight(request.getWeight());
        if (request.getThresholdValue() != null) rule.setThresholdValue(request.getThresholdValue());
        if (request.getTimeWindowSeconds() != null) rule.setTimeWindowSeconds(request.getTimeWindowSeconds());
        if (request.getIsActive() != null) rule.setActive(request.getIsActive());

        log.info("Updated fraud rule [{}] (Code: {})", rule.getRuleName(), rule.getRuleCode());
        return fraudRuleRepository.save(rule);
    }

    private RiskAssessmentDto mapToDto(RiskAssessment entity) {
        List<TriggeredRuleDto> triggeredRuleDtos = entity.getTriggeredRules().stream()
                .map(tr -> TriggeredRuleDto.builder()
                        .ruleCode(tr.getRuleCode())
                        .ruleName(tr.getRuleName())
                        .pointsAdded(tr.getPointsAdded())
                        .triggerReason(tr.getTriggerReason())
                        .build())
                .collect(Collectors.toList());

        return RiskAssessmentDto.builder()
                .id(entity.getId())
                .transactionId(entity.getTransactionId())
                .accountId(entity.getAccountId())
                .amount(entity.getAmount())
                .currency(entity.getCurrency())
                .riskScore(entity.getRiskScore())
                .riskLevel(entity.getRiskLevel())
                .actionRecommended(entity.getActionRecommended())
                .triggeredRules(triggeredRuleDtos)
                .evaluationTimestamp(entity.getEvaluationTimestamp())
                .build();
    }
}

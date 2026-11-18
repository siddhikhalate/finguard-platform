package com.finguard.incident.service;

import com.finguard.common.constant.KafkaTopics;
import com.finguard.common.enums.IncidentPriority;
import com.finguard.common.enums.IncidentStatus;
import com.finguard.common.enums.RiskLevel;
import com.finguard.common.event.FraudIncidentCreatedEvent;
import com.finguard.common.event.FraudRiskAssessedEvent;
import com.finguard.incident.dto.AssignIncidentRequest;
import com.finguard.incident.dto.AuditLogDto;
import com.finguard.incident.dto.IncidentDetailDto;
import com.finguard.incident.dto.IncidentSummaryDto;
import com.finguard.incident.dto.UpdateIncidentStatusRequest;
import com.finguard.incident.entity.FraudIncident;
import com.finguard.incident.entity.IncidentAuditLog;
import com.finguard.incident.repository.FraudIncidentRepository;
import com.finguard.incident.repository.IncidentAuditLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class IncidentService {

    private final FraudIncidentRepository incidentRepository;
    private final IncidentAuditLogRepository auditLogRepository;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Transactional
    public Optional<FraudIncident> processRiskAssessedEvent(FraudRiskAssessedEvent event) {
        // Incident threshold: Risk Score >= 50 (HIGH or CRITICAL)
        if (event.getRiskScore() < 50 && event.getRiskLevel() != RiskLevel.HIGH && event.getRiskLevel() != RiskLevel.CRITICAL) {
            log.debug("Transaction {} has risk score {} ({}). Incident creation not required.",
                    event.getTransactionId(), event.getRiskScore(), event.getRiskLevel());
            return Optional.empty();
        }

        // Idempotency check: prevent duplicate incident creation for the same transaction
        if (incidentRepository.existsByTransactionId(event.getTransactionId())) {
            log.info("Incident already exists for transaction {}. Skipping duplicate Kafka event.", event.getTransactionId());
            return incidentRepository.findByTransactionId(event.getTransactionId());
        }

        String incidentRef = "INC-" + (System.currentTimeMillis() % 10000000);
        IncidentPriority priority = (event.getRiskLevel() == RiskLevel.CRITICAL)
                ? IncidentPriority.URGENT
                : IncidentPriority.HIGH;

        FraudIncident incident = FraudIncident.builder()
                .incidentReference(incidentRef)
                .transactionId(event.getTransactionId())
                .accountId(event.getAccountId())
                .amount(event.getAmount())
                .currency(event.getCurrency() != null ? event.getCurrency() : "USD")
                .location(event.getLocation())
                .riskScore(event.getRiskScore())
                .riskLevel(event.getRiskLevel())
                .status(IncidentStatus.NEW)
                .priority(priority)
                .build();

        FraudIncident savedIncident = incidentRepository.save(incident);

        // Record initial audit trail log
        IncidentAuditLog auditLog = IncidentAuditLog.builder()
                .incident(savedIncident)
                .actorUsername("SYSTEM_AUTOMATION")
                .action("AUTO_CREATED")
                .comment(String.format("Incident auto-created due to %s risk score: %d/100",
                        event.getRiskLevel(), event.getRiskScore()))
                .previousStatus(null)
                .newStatus(IncidentStatus.NEW.name())
                .build();

        auditLogRepository.save(auditLog);
        log.info("Created fraud incident {} (Ref: {}) for transaction {}",
                savedIncident.getId(), incidentRef, event.getTransactionId());

        // Emit FraudIncidentCreatedEvent to Kafka (for AI Explanation Service)
        FraudIncidentCreatedEvent incidentEvent = FraudIncidentCreatedEvent.builder()
                .eventId(UUID.randomUUID().toString())
                .eventType("FRAUD_INCIDENT_CREATED")
                .correlationId(event.getCorrelationId())
                .timestamp(Instant.now())
                .incidentId(savedIncident.getId())
                .incidentReference(savedIncident.getIncidentReference())
                .transactionId(savedIncident.getTransactionId())
                .accountId(savedIncident.getAccountId())
                .amount(savedIncident.getAmount())
                .currency(savedIncident.getCurrency())
                .location(savedIncident.getLocation())
                .riskScore(savedIncident.getRiskScore())
                .riskLevel(savedIncident.getRiskLevel())
                .priority(savedIncident.getPriority())
                .status(savedIncident.getStatus())
                .triggeredRules(event.getTriggeredRules())
                .build();

        kafkaTemplate.send(KafkaTopics.FRAUD_INCIDENT_CREATED, savedIncident.getAccountId().toString(), incidentEvent);
        log.info("Emitted FraudIncidentCreatedEvent to Kafka topic {}", KafkaTopics.FRAUD_INCIDENT_CREATED);

        return Optional.of(savedIncident);
    }

    @Transactional
    public IncidentSummaryDto assignIncident(UUID incidentId, AssignIncidentRequest request, String actorUsername) {
        FraudIncident incident = incidentRepository.findById(incidentId)
                .orElseThrow(() -> new RuntimeException("Incident not found with ID: " + incidentId));

        String prevStatus = incident.getStatus().name();
        incident.setAssignedTo(request.getAnalystId());
        incident.setAssignedToUsername(request.getAnalystUsername());
        incident.setStatus(IncidentStatus.UNDER_INVESTIGATION);

        FraudIncident updated = incidentRepository.save(incident);

        IncidentAuditLog logEntry = IncidentAuditLog.builder()
                .incident(updated)
                .actorUsername(actorUsername != null ? actorUsername : "ANALYST")
                .action("ASSIGNED")
                .comment("Assigned investigation to analyst: " + request.getAnalystUsername())
                .previousStatus(prevStatus)
                .newStatus(IncidentStatus.UNDER_INVESTIGATION.name())
                .build();

        auditLogRepository.save(logEntry);
        return mapToSummary(updated);
    }

    @Transactional
    public IncidentSummaryDto updateIncidentStatus(
            UUID incidentId,
            UpdateIncidentStatusRequest request,
            UUID actorId,
            String actorUsername) {

        FraudIncident incident = incidentRepository.findById(incidentId)
                .orElseThrow(() -> new RuntimeException("Incident not found with ID: " + incidentId));

        String prevStatus = incident.getStatus().name();
        incident.setStatus(request.getStatus());
        if (request.getNotes() != null) {
            incident.setResolutionNotes(request.getNotes());
        }

        FraudIncident updated = incidentRepository.save(incident);

        IncidentAuditLog logEntry = IncidentAuditLog.builder()
                .incident(updated)
                .actorId(actorId)
                .actorUsername(actorUsername != null ? actorUsername : "ANALYST")
                .action("STATUS_CHANGE")
                .comment(request.getNotes() != null ? request.getNotes() : "Status updated to " + request.getStatus())
                .previousStatus(prevStatus)
                .newStatus(request.getStatus().name())
                .build();

        auditLogRepository.save(logEntry);
        return mapToSummary(updated);
    }

    @Transactional(readOnly = true)
    public Page<IncidentSummaryDto> getIncidents(IncidentStatus status, Pageable pageable) {
        if (status != null) {
            return incidentRepository.findByStatus(status, pageable).map(this::mapToSummary);
        }
        return incidentRepository.findAll(pageable).map(this::mapToSummary);
    }

    @Transactional(readOnly = true)
    public IncidentDetailDto getIncidentById(UUID incidentId) {
        FraudIncident incident = incidentRepository.findById(incidentId)
                .orElseThrow(() -> new RuntimeException("Incident not found with ID: " + incidentId));

        List<AuditLogDto> auditDtos = auditLogRepository.findByIncidentIdOrderByCreatedAtDesc(incidentId).stream()
                .map(a -> AuditLogDto.builder()
                        .id(a.getId())
                        .actorUsername(a.getActorUsername())
                        .action(a.getAction())
                        .comment(a.getComment())
                        .previousStatus(a.getPreviousStatus())
                        .newStatus(a.getNewStatus())
                        .createdAt(a.getCreatedAt())
                        .build())
                .collect(Collectors.toList());

        IncidentDetailDto.AiExplanationDto aiDto = null;
        if (incident.getAiExplanation() != null) {
            aiDto = IncidentDetailDto.AiExplanationDto.builder()
                    .summary(incident.getAiExplanation().getSummary())
                    .factors(incident.getAiExplanation().getFactors())
                    .recommendedActions(incident.getAiExplanation().getRecommendedActions())
                    .confidenceLevel(incident.getAiExplanation().getConfidenceLevel())
                    .modelUsed(incident.getAiExplanation().getModelUsed())
                    .isFallback(incident.getAiExplanation().isFallback())
                    .build();
        }

        return IncidentDetailDto.builder()
                .id(incident.getId())
                .incidentReference(incident.getIncidentReference())
                .transactionId(incident.getTransactionId())
                .accountId(incident.getAccountId())
                .amount(incident.getAmount())
                .currency(incident.getCurrency())
                .location(incident.getLocation())
                .riskScore(incident.getRiskScore())
                .riskLevel(incident.getRiskLevel())
                .status(incident.getStatus())
                .priority(incident.getPriority())
                .assignedTo(incident.getAssignedTo())
                .assignedToUsername(incident.getAssignedToUsername())
                .resolutionNotes(incident.getResolutionNotes())
                .aiExplanation(aiDto)
                .auditLogs(auditDtos)
                .createdAt(incident.getCreatedAt())
                .updatedAt(incident.getUpdatedAt())
                .build();
    }

    private IncidentSummaryDto mapToSummary(FraudIncident inc) {
        return IncidentSummaryDto.builder()
                .id(inc.getId())
                .incidentReference(inc.getIncidentReference())
                .transactionId(inc.getTransactionId())
                .accountId(inc.getAccountId())
                .amount(inc.getAmount())
                .currency(inc.getCurrency())
                .location(inc.getLocation())
                .riskScore(inc.getRiskScore())
                .riskLevel(inc.getRiskLevel())
                .status(inc.getStatus())
                .priority(inc.getPriority())
                .assignedTo(inc.getAssignedTo())
                .assignedToUsername(inc.getAssignedToUsername())
                .createdAt(inc.getCreatedAt())
                .updatedAt(inc.getUpdatedAt())
                .build();
    }
}

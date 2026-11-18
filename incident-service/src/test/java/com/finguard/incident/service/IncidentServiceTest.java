package com.finguard.incident.service;

import com.finguard.common.constant.KafkaTopics;
import com.finguard.common.enums.IncidentPriority;
import com.finguard.common.enums.IncidentStatus;
import com.finguard.common.enums.RiskLevel;
import com.finguard.common.event.FraudIncidentCreatedEvent;
import com.finguard.common.event.FraudRiskAssessedEvent;
import com.finguard.incident.dto.AssignIncidentRequest;
import com.finguard.incident.dto.IncidentSummaryDto;
import com.finguard.incident.dto.UpdateIncidentStatusRequest;
import com.finguard.incident.entity.FraudIncident;
import com.finguard.incident.entity.IncidentAuditLog;
import com.finguard.incident.repository.FraudIncidentRepository;
import com.finguard.incident.repository.IncidentAuditLogRepository;
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
class IncidentServiceTest {

    @Mock
    private FraudIncidentRepository incidentRepository;

    @Mock
    private IncidentAuditLogRepository auditLogRepository;

    @Mock
    private KafkaTemplate<String, Object> kafkaTemplate;

    @InjectMocks
    private IncidentService incidentService;

    private UUID transactionId;
    private UUID accountId;

    @BeforeEach
    void setUp() {
        transactionId = UUID.randomUUID();
        accountId = UUID.randomUUID();
    }

    @Test
    @DisplayName("Should auto-create incident for CRITICAL risk transaction and publish event to Kafka")
    void shouldCreateIncidentForCriticalRisk() {
        FraudRiskAssessedEvent event = FraudRiskAssessedEvent.builder()
                .assessmentId(UUID.randomUUID())
                .transactionId(transactionId)
                .accountId(accountId)
                .amount(new BigDecimal("25000.00"))
                .currency("USD")
                .location("Lagos, Nigeria")
                .riskScore(80)
                .riskLevel(RiskLevel.CRITICAL)
                .actionRecommended("BLOCK_AND_FREEZE_ACCOUNT")
                .triggeredRules(List.of())
                .correlationId("corr-crit")
                .build();

        when(incidentRepository.existsByTransactionId(transactionId)).thenReturn(false);
        when(incidentRepository.save(any(FraudIncident.class))).thenAnswer(inv -> {
            FraudIncident inc = inv.getArgument(0);
            inc.setId(UUID.randomUUID());
            return inc;
        });
        when(kafkaTemplate.send(any(), any(), any())).thenReturn(new CompletableFuture<>());

        Optional<FraudIncident> result = incidentService.processRiskAssessedEvent(event);

        assertTrue(result.isPresent());
        FraudIncident created = result.get();
        assertEquals(IncidentPriority.URGENT, created.getPriority());
        assertEquals(IncidentStatus.NEW, created.getStatus());
        assertEquals(80, created.getRiskScore());

        // Verify audit log recorded
        verify(auditLogRepository).save(any(IncidentAuditLog.class));

        // Verify Kafka event published to topic fraud-incident-created
        ArgumentCaptor<FraudIncidentCreatedEvent> captor = ArgumentCaptor.forClass(FraudIncidentCreatedEvent.class);
        verify(kafkaTemplate).send(eq(KafkaTopics.FRAUD_INCIDENT_CREATED), eq(accountId.toString()), captor.capture());
        assertEquals(IncidentPriority.URGENT, captor.getValue().getPriority());
    }

    @Test
    @DisplayName("Should skip incident creation for LOW risk transaction")
    void shouldSkipIncidentForLowRisk() {
        FraudRiskAssessedEvent event = FraudRiskAssessedEvent.builder()
                .transactionId(transactionId)
                .accountId(accountId)
                .amount(new BigDecimal("50.00"))
                .riskScore(10)
                .riskLevel(RiskLevel.LOW)
                .build();

        Optional<FraudIncident> result = incidentService.processRiskAssessedEvent(event);

        assertTrue(result.isEmpty());
        verify(incidentRepository, never()).save(any());
        verify(kafkaTemplate, never()).send(any(), any(), any());
    }

    @Test
    @DisplayName("Should assign incident to analyst and transition status to UNDER_INVESTIGATION")
    void shouldAssignIncidentSuccessfully() {
        UUID incidentId = UUID.randomUUID();
        UUID analystId = UUID.randomUUID();

        FraudIncident incident = FraudIncident.builder()
                .id(incidentId)
                .incidentReference("INC-1002")
                .status(IncidentStatus.NEW)
                .riskScore(60)
                .riskLevel(RiskLevel.HIGH)
                .amount(new BigDecimal("15000.00"))
                .build();

        when(incidentRepository.findById(incidentId)).thenReturn(Optional.of(incident));
        when(incidentRepository.save(any())).thenReturn(incident);

        AssignIncidentRequest request = AssignIncidentRequest.builder()
                .analystId(analystId)
                .analystUsername("sarah_connor")
                .build();

        IncidentSummaryDto summary = incidentService.assignIncident(incidentId, request, "lead_supervisor");

        assertNotNull(summary);
        assertEquals(IncidentStatus.UNDER_INVESTIGATION, incident.getStatus());
        assertEquals(analystId, incident.getAssignedTo());
        assertEquals("sarah_connor", incident.getAssignedToUsername());

        // Verify assignment audit log
        verify(auditLogRepository).save(any(IncidentAuditLog.class));
    }

    @Test
    @DisplayName("Should update incident status to RESOLVED with resolution notes")
    void shouldUpdateStatusSuccessfully() {
        UUID incidentId = UUID.randomUUID();
        UUID actorId = UUID.randomUUID();

        FraudIncident incident = FraudIncident.builder()
                .id(incidentId)
                .incidentReference("INC-1003")
                .status(IncidentStatus.UNDER_INVESTIGATION)
                .riskScore(60)
                .riskLevel(RiskLevel.HIGH)
                .amount(new BigDecimal("15000.00"))
                .build();

        when(incidentRepository.findById(incidentId)).thenReturn(Optional.of(incident));
        when(incidentRepository.save(any())).thenReturn(incident);

        UpdateIncidentStatusRequest request = UpdateIncidentStatusRequest.builder()
                .status(IncidentStatus.RESOLVED)
                .notes("Verified with cardholder. Transaction confirmed legitimate.")
                .build();

        IncidentSummaryDto summary = incidentService.updateIncidentStatus(incidentId, request, actorId, "sarah_connor");

        assertNotNull(summary);
        assertEquals(IncidentStatus.RESOLVED, incident.getStatus());
        assertEquals("Verified with cardholder. Transaction confirmed legitimate.", incident.getResolutionNotes());
        verify(auditLogRepository).save(any(IncidentAuditLog.class));
    }
}

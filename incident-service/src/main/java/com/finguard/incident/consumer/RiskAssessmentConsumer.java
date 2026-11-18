package com.finguard.incident.consumer;

import com.finguard.common.constant.KafkaTopics;
import com.finguard.common.event.FraudRiskAssessedEvent;
import com.finguard.incident.service.IncidentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class RiskAssessmentConsumer {

    private final IncidentService incidentService;

    @KafkaListener(
            topics = KafkaTopics.FRAUD_RISK_ASSESSED,
            groupId = "incident-management-group",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void consumeRiskAssessed(@Payload FraudRiskAssessedEvent event) {
        log.info("Received FraudRiskAssessedEvent for txn: {} with RiskScore: {} ({})",
                event.getTransactionId(), event.getRiskScore(), event.getRiskLevel());

        try {
            incidentService.processRiskAssessedEvent(event);
        } catch (Exception ex) {
            log.error("Failed to process risk assessment event for incident triage: {}", ex.getMessage(), ex);
            throw ex;
        }
    }
}

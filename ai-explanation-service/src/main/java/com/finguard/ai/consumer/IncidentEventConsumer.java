package com.finguard.ai.consumer;

import com.finguard.ai.dto.AiExplanationRequest;
import com.finguard.ai.service.AiExplanationService;
import com.finguard.common.constant.KafkaTopics;
import com.finguard.common.event.FraudIncidentCreatedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class IncidentEventConsumer {

    private final AiExplanationService aiExplanationService;

    @KafkaListener(
            topics = KafkaTopics.FRAUD_INCIDENT_CREATED,
            groupId = "ai-explanation-group",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void consumeIncidentCreated(@Payload FraudIncidentCreatedEvent event) {
        log.info("Received Kafka FraudIncidentCreatedEvent for Incident: {} (Txn: {})",
                event.getIncidentReference(), event.getTransactionId());

        try {
            AiExplanationRequest request = AiExplanationRequest.builder()
                    .incidentId(event.getIncidentId())
                    .transactionId(event.getTransactionId())
                    .accountId(event.getAccountId())
                    .amount(event.getAmount())
                    .currency(event.getCurrency())
                    .location(event.getLocation())
                    .riskScore(event.getRiskScore())
                    .riskLevel(event.getRiskLevel())
                    .triggeredRules(event.getTriggeredRules())
                    .build();

            aiExplanationService.generateExplanation(request);
        } catch (Exception ex) {
            log.error("Failed to generate AI explanation for incident {}: {}",
                    event.getIncidentId(), ex.getMessage(), ex);
        }
    }
}

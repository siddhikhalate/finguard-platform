package com.finguard.fraud.consumer;

import com.finguard.common.constant.KafkaTopics;
import com.finguard.common.event.TransactionCreatedEvent;
import com.finguard.fraud.service.FraudDetectionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class TransactionEventConsumer {

    private final FraudDetectionService fraudDetectionService;

    @KafkaListener(
            topics = KafkaTopics.TRANSACTION_CREATED,
            groupId = "fraud-detection-group",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void consumeTransactionCreated(@Payload TransactionCreatedEvent event) {
        log.info("Received Kafka transaction-created event for transaction: {} (Account: {})",
                event.getTransactionId(), event.getAccountId());

        try {
            fraudDetectionService.assessTransaction(event);
        } catch (Exception ex) {
            log.error("Failed to evaluate fraud rules for transaction {}: {}",
                    event.getTransactionId(), ex.getMessage(), ex);
            throw ex; // Re-throw to allow Kafka retry / dead-letter handling
        }
    }
}

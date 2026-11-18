package com.finguard.transaction.outbox;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.finguard.common.constant.KafkaTopics;
import com.finguard.common.event.TransactionCreatedEvent;
import com.finguard.transaction.entity.OutboxEvent;
import com.finguard.transaction.repository.OutboxEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class OutboxPublisher {

    private final OutboxEventRepository outboxEventRepository;
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final ObjectMapper objectMapper;

    @Value("${finguard.outbox.batch-size:50}")
    private int batchSize;

    @Scheduled(fixedDelayString = "${finguard.outbox.polling-rate-ms:1000}")
    @Transactional
    public void publishPendingEvents() {
        List<OutboxEvent> pendingEvents = outboxEventRepository.findByStatusOrderByCreatedAtAsc(
                "PENDING", PageRequest.of(0, batchSize));

        if (pendingEvents.isEmpty()) {
            return;
        }

        log.debug("Found {} pending outbox events to publish to Kafka", pendingEvents.size());

        for (OutboxEvent event : pendingEvents) {
            try {
                TransactionCreatedEvent domainEvent = objectMapper.readValue(
                        event.getPayload(), TransactionCreatedEvent.class);

                // Use accountId as the Kafka partition key to guarantee strictly ordered processing per account
                String partitionKey = domainEvent.getAccountId().toString();

                kafkaTemplate.send(KafkaTopics.TRANSACTION_CREATED, partitionKey, domainEvent)
                        .whenComplete((result, ex) -> {
                            if (ex == null) {
                                event.setStatus("PUBLISHED");
                                event.setProcessedAt(Instant.now());
                                outboxEventRepository.save(event);
                                log.info("Successfully published outbox event {} for txn {} to Kafka topic {}",
                                        event.getId(), event.getAggregateId(), KafkaTopics.TRANSACTION_CREATED);
                            } else {
                                event.setRetryCount(event.getRetryCount() + 1);
                                if (event.getRetryCount() >= 5) {
                                    event.setStatus("FAILED");
                                    log.error("Outbox event {} exceeded max retries. Marked as FAILED.", event.getId(), ex);
                                } else {
                                    log.warn("Failed to publish outbox event {}. Retry count: {}", event.getId(), event.getRetryCount(), ex);
                                }
                                outboxEventRepository.save(event);
                            }
                        });

            } catch (Exception e) {
                log.error("Error preparing outbox event {} for Kafka: {}", event.getId(), e.getMessage());
                event.setRetryCount(event.getRetryCount() + 1);
                outboxEventRepository.save(event);
            }
        }
    }
}

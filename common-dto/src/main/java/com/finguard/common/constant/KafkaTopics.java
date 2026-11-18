package com.finguard.common.constant;

/**
 * Centrally defined Kafka topic names to avoid typos across microservices.
 */
public final class KafkaTopics {

    private KafkaTopics() {
        // Prevent instantiation
    }

    public static final String TRANSACTION_CREATED = "finguard.transaction-created";
    public static final String FRAUD_RISK_ASSESSED = "finguard.fraud-risk-assessed";
    public static final String FRAUD_INCIDENT_CREATED = "finguard.fraud-incident-created";
    public static final String DEAD_LETTER_QUEUE = "finguard.dead-letter-queue";
}

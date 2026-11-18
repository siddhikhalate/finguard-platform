package com.finguard.common.event;

import com.finguard.common.enums.TransactionStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Event published when a new transaction is successfully saved to the Transaction Service database.
 * Handled by the Fraud Detection Service.
 */
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class TransactionCreatedEvent extends BaseEvent {

    private UUID transactionId;
    private UUID accountId;
    private String sourceAccount;
    private String targetAccount;
    private BigDecimal amount;
    private String currency;
    private String channel;
    private String location;
    private String ipAddress;
    private TransactionStatus status;
    private Instant transactionTime;
}

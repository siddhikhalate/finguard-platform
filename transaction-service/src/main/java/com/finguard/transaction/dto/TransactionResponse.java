package com.finguard.transaction.dto;

import com.finguard.common.enums.TransactionStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TransactionResponse {

    private UUID transactionId;
    private String idempotencyKey;
    private UUID accountId;
    private String sourceAccount;
    private String targetAccount;
    private BigDecimal amount;
    private String currency;
    private String channel;
    private String location;
    private String ipAddress;
    private TransactionStatus status;
    private BigDecimal remainingBalance;
    private Instant createdAt;
}

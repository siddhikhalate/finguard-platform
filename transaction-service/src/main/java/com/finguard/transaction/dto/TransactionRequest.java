package com.finguard.transaction.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TransactionRequest {

    private String idempotencyKey;

    @NotNull(message = "Account ID is required")
    private UUID accountId;

    @NotBlank(message = "Source account number is required")
    private String sourceAccount;

    @NotBlank(message = "Target account number is required")
    private String targetAccount;

    @NotNull(message = "Transaction amount is required")
    @DecimalMin(value = "0.01", message = "Amount must be strictly greater than 0")
    private BigDecimal amount;

    @Builder.Default
    private String currency = "USD";

    @NotBlank(message = "Channel is required (e.g. ONLINE_TRANSFER, MOBILE_APP, POS, ATM)")
    private String channel;

    @NotBlank(message = "Location is required (e.g. City, Country)")
    private String location;

    private String ipAddress;
}

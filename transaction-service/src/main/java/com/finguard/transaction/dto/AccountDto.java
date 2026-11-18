package com.finguard.transaction.dto;

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
public class AccountDto {

    private UUID id;
    private String accountNumber;
    private String cardholderName;
    private BigDecimal balance;
    private String currency;
    private String status;
    private Instant createdAt;
}

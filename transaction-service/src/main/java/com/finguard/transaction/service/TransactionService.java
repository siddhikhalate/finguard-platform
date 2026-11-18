package com.finguard.transaction.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.finguard.common.enums.TransactionStatus;
import com.finguard.common.event.TransactionCreatedEvent;
import com.finguard.transaction.dto.AccountDto;
import com.finguard.transaction.dto.TransactionRequest;
import com.finguard.transaction.dto.TransactionResponse;
import com.finguard.transaction.entity.Account;
import com.finguard.transaction.entity.OutboxEvent;
import com.finguard.transaction.entity.Transaction;
import com.finguard.transaction.exception.ResourceNotFoundException;
import com.finguard.transaction.repository.AccountRepository;
import com.finguard.transaction.repository.OutboxEventRepository;
import com.finguard.transaction.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class TransactionService {

    private final TransactionRepository transactionRepository;
    private final AccountRepository accountRepository;
    private final OutboxEventRepository outboxEventRepository;
    private final ObjectMapper objectMapper;

    @Transactional
    public TransactionResponse processTransaction(
            TransactionRequest request,
            String idempotencyKeyHeader,
            String correlationId) {

        String idempotencyKey = StringUtils.hasText(idempotencyKeyHeader)
                ? idempotencyKeyHeader
                : (StringUtils.hasText(request.getIdempotencyKey()) ? request.getIdempotencyKey() : UUID.randomUUID().toString());

        // 1. Idempotency Check: prevent duplicate financial transactions
        Optional<Transaction> existingTxn = transactionRepository.findByIdempotencyKey(idempotencyKey);
        if (existingTxn.isPresent()) {
            log.info("Duplicate transaction request with Idempotency-Key: {}. Returning existing record.", idempotencyKey);
            return mapToResponse(existingTxn.get(), existingTxn.get().getAccount().getBalance());
        }

        // 2. Account Validation
        Account account = accountRepository.findById(request.getAccountId())
                .orElseThrow(() -> new ResourceNotFoundException("Account not found with ID: " + request.getAccountId()));

        // 3. Balance Evaluation & Transaction Status Determination
        TransactionStatus status;
        if (account.getBalance().compareTo(request.getAmount()) < 0) {
            status = TransactionStatus.FAILED;
            log.warn("Account {} has insufficient balance ({} < {})",
                    account.getAccountNumber(), account.getBalance(), request.getAmount());
        } else {
            status = TransactionStatus.COMPLETED;
            account.setBalance(account.getBalance().subtract(request.getAmount()));
            accountRepository.save(account);
        }

        // 4. Save Transaction Record
        Transaction transaction = Transaction.builder()
                .idempotencyKey(idempotencyKey)
                .account(account)
                .sourceAccount(request.getSourceAccount())
                .targetAccount(request.getTargetAccount())
                .amount(request.getAmount())
                .currency(request.getCurrency())
                .channel(request.getChannel())
                .location(request.getLocation())
                .ipAddress(request.getIpAddress())
                .status(status)
                .build();

        Transaction savedTxn = transactionRepository.save(transaction);

        // 5. Build and Save Transactional Outbox Event (Atomic with Transaction Save)
        TransactionCreatedEvent event = TransactionCreatedEvent.builder()
                .eventId(UUID.randomUUID().toString())
                .eventType("TRANSACTION_CREATED")
                .correlationId(StringUtils.hasText(correlationId) ? correlationId : UUID.randomUUID().toString())
                .timestamp(Instant.now())
                .transactionId(savedTxn.getId())
                .accountId(account.getId())
                .sourceAccount(savedTxn.getSourceAccount())
                .targetAccount(savedTxn.getTargetAccount())
                .amount(savedTxn.getAmount())
                .currency(savedTxn.getCurrency())
                .channel(savedTxn.getChannel())
                .location(savedTxn.getLocation())
                .ipAddress(savedTxn.getIpAddress())
                .status(savedTxn.getStatus())
                .transactionTime(savedTxn.getCreatedAt())
                .build();

        try {
            String payloadJson = objectMapper.writeValueAsString(event);
            OutboxEvent outboxEvent = OutboxEvent.builder()
                    .aggregateType("TRANSACTION")
                    .aggregateId(savedTxn.getId().toString())
                    .eventType("TRANSACTION_CREATED")
                    .payload(payloadJson)
                    .status("PENDING")
                    .build();

            outboxEventRepository.save(outboxEvent);
            log.info("Saved transaction {} and queued outbox event for Kafka dispatch", savedTxn.getId());
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize TransactionCreatedEvent to JSON", e);
            throw new RuntimeException("Outbox serialization failure", e);
        }

        return mapToResponse(savedTxn, account.getBalance());
    }

    @Transactional(readOnly = true)
    public TransactionResponse getTransactionById(UUID id) {
        Transaction transaction = transactionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Transaction not found with ID: " + id));

        return mapToResponse(transaction, transaction.getAccount().getBalance());
    }

    @Transactional(readOnly = true)
    public Page<TransactionResponse> getTransactionsByAccount(UUID accountId, Pageable pageable) {
        return transactionRepository.findByAccountIdOrderByCreatedAtDesc(accountId, pageable)
                .map(txn -> mapToResponse(txn, txn.getAccount().getBalance()));
    }

    @Transactional(readOnly = true)
    public List<AccountDto> getAllAccounts() {
        return accountRepository.findAll().stream()
                .map(acc -> AccountDto.builder()
                        .id(acc.getId())
                        .accountNumber(acc.getAccountNumber())
                        .cardholderName(acc.getCardholderName())
                        .balance(acc.getBalance())
                        .currency(acc.getCurrency())
                        .status(acc.getStatus())
                        .createdAt(acc.getCreatedAt())
                        .build())
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public AccountDto getAccountById(UUID accountId) {
        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new ResourceNotFoundException("Account not found with ID: " + accountId));

        return AccountDto.builder()
                .id(account.getId())
                .accountNumber(account.getAccountNumber())
                .cardholderName(account.getCardholderName())
                .balance(account.getBalance())
                .currency(account.getCurrency())
                .status(account.getStatus())
                .createdAt(account.getCreatedAt())
                .build();
    }

    private TransactionResponse mapToResponse(Transaction txn, java.math.BigDecimal balance) {
        return TransactionResponse.builder()
                .transactionId(txn.getId())
                .idempotencyKey(txn.getIdempotencyKey())
                .accountId(txn.getAccount().getId())
                .sourceAccount(txn.getSourceAccount())
                .targetAccount(txn.getTargetAccount())
                .amount(txn.getAmount())
                .currency(txn.getCurrency())
                .channel(txn.getChannel())
                .location(txn.getLocation())
                .ipAddress(txn.getIpAddress())
                .status(txn.getStatus())
                .remainingBalance(balance)
                .createdAt(txn.getCreatedAt())
                .build();
    }
}

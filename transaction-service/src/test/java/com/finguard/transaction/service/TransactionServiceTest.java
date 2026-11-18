package com.finguard.transaction.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.finguard.common.enums.TransactionStatus;
import com.finguard.transaction.dto.TransactionRequest;
import com.finguard.transaction.dto.TransactionResponse;
import com.finguard.transaction.entity.Account;
import com.finguard.transaction.entity.OutboxEvent;
import com.finguard.transaction.entity.Transaction;
import com.finguard.transaction.exception.ResourceNotFoundException;
import com.finguard.transaction.repository.AccountRepository;
import com.finguard.transaction.repository.OutboxEventRepository;
import com.finguard.transaction.repository.TransactionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TransactionServiceTest {

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private OutboxEventRepository outboxEventRepository;

    @Spy
    private ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new com.fasterxml.jackson.datatype.jsr310.JavaTimeModule())
            .disable(com.fasterxml.jackson.databind.SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    @InjectMocks
    private TransactionService transactionService;

    private UUID accountId;
    private Account account;

    @BeforeEach
    void setUp() {
        accountId = UUID.randomUUID();
        account = Account.builder()
                .id(accountId)
                .accountNumber("ACC-100201")
                .cardholderName("Alice Walker")
                .balance(new BigDecimal("1000.00"))
                .currency("USD")
                .status("ACTIVE")
                .createdAt(Instant.now())
                .build();
    }

    @Test
    @DisplayName("Should successfully process transaction, deduct balance, and queue outbox event")
    void shouldProcessTransactionSuccessfully() {
        TransactionRequest request = TransactionRequest.builder()
                .accountId(accountId)
                .sourceAccount("ACC-100201")
                .targetAccount("ACC-500999")
                .amount(new BigDecimal("250.00"))
                .currency("USD")
                .channel("ONLINE_TRANSFER")
                .location("London, UK")
                .build();

        when(accountRepository.findById(accountId)).thenReturn(Optional.of(account));
        when(transactionRepository.save(any(Transaction.class))).thenAnswer(invocation -> {
            Transaction txn = invocation.getArgument(0);
            txn.setId(UUID.randomUUID());
            return txn;
        });

        TransactionResponse response = transactionService.processTransaction(request, "idemp-key-1", "corr-id-1");

        assertNotNull(response);
        assertEquals(TransactionStatus.COMPLETED, response.getStatus());
        assertEquals(new BigDecimal("750.00"), account.getBalance());
        assertEquals(new BigDecimal("750.00"), response.getRemainingBalance());

        // Verify account balance update saved
        verify(accountRepository).save(account);

        // Verify Outbox event created
        ArgumentCaptor<OutboxEvent> outboxCaptor = ArgumentCaptor.forClass(OutboxEvent.class);
        verify(outboxEventRepository).save(outboxCaptor.capture());
        OutboxEvent outboxEvent = outboxCaptor.getValue();
        assertEquals("PENDING", outboxEvent.getStatus());
        assertEquals("TRANSACTION_CREATED", outboxEvent.getEventType());
    }

    @Test
    @DisplayName("Should mark transaction as FAILED when balance is insufficient, without balance deduction")
    void shouldMarkTransactionAsFailedWhenBalanceInsufficient() {
        TransactionRequest request = TransactionRequest.builder()
                .accountId(accountId)
                .sourceAccount("ACC-100201")
                .targetAccount("ACC-500999")
                .amount(new BigDecimal("5000.00")) // Exceeds 1000.00
                .currency("USD")
                .channel("ATM")
                .location("Paris, FR")
                .build();

        when(accountRepository.findById(accountId)).thenReturn(Optional.of(account));
        when(transactionRepository.save(any(Transaction.class))).thenAnswer(invocation -> {
            Transaction txn = invocation.getArgument(0);
            txn.setId(UUID.randomUUID());
            return txn;
        });

        TransactionResponse response = transactionService.processTransaction(request, "idemp-key-2", "corr-id-2");

        assertNotNull(response);
        assertEquals(TransactionStatus.FAILED, response.getStatus());
        assertEquals(new BigDecimal("1000.00"), account.getBalance()); // Unchanged

        // Outbox event should still be saved so fraud detection can track failed transaction velocity!
        verify(outboxEventRepository).save(any(OutboxEvent.class));
    }

    @Test
    @DisplayName("Should return existing transaction when duplicate Idempotency-Key is received")
    void shouldReturnExistingTransactionForDuplicateIdempotencyKey() {
        Transaction existingTxn = Transaction.builder()
                .id(UUID.randomUUID())
                .idempotencyKey("idemp-duplicate-key")
                .account(account)
                .sourceAccount("ACC-100201")
                .targetAccount("ACC-500999")
                .amount(new BigDecimal("100.00"))
                .currency("USD")
                .channel("POS")
                .location("Berlin, DE")
                .status(TransactionStatus.COMPLETED)
                .build();

        when(transactionRepository.findByIdempotencyKey("idemp-duplicate-key")).thenReturn(Optional.of(existingTxn));

        TransactionRequest request = TransactionRequest.builder()
                .accountId(accountId)
                .sourceAccount("ACC-100201")
                .targetAccount("ACC-500999")
                .amount(new BigDecimal("100.00"))
                .currency("USD")
                .channel("POS")
                .location("Berlin, DE")
                .build();

        TransactionResponse response = transactionService.processTransaction(request, "idemp-duplicate-key", "corr-3");

        assertNotNull(response);
        assertEquals(existingTxn.getId(), response.getTransactionId());
        // Verify no extra outbox event was created
        verify(outboxEventRepository, never()).save(any());
        // Verify account balance wasn't deducted again
        verify(accountRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should throw ResourceNotFoundException when account does not exist")
    void shouldThrowWhenAccountNotFound() {
        UUID unknownAccountId = UUID.randomUUID();
        when(accountRepository.findById(unknownAccountId)).thenReturn(Optional.empty());

        TransactionRequest request = TransactionRequest.builder()
                .accountId(unknownAccountId)
                .sourceAccount("UNKNOWN")
                .targetAccount("ACC-999")
                .amount(new BigDecimal("100.00"))
                .currency("USD")
                .channel("ONLINE_TRANSFER")
                .location("Toronto, CA")
                .build();

        assertThrows(ResourceNotFoundException.class, () ->
                transactionService.processTransaction(request, "idemp-4", "corr-4"));
    }
}

package com.finguard.transaction.controller;

import com.finguard.common.constant.TracingConstants;
import com.finguard.common.dto.ApiResponse;
import com.finguard.transaction.dto.AccountDto;
import com.finguard.transaction.dto.TransactionRequest;
import com.finguard.transaction.dto.TransactionResponse;
import com.finguard.transaction.service.TransactionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/v1/transactions")
@RequiredArgsConstructor
@Tag(name = "Financial Transactions", description = "Endpoints for submitting, validating, and retrieving financial transactions")
public class TransactionController {

    private final TransactionService transactionService;

    @PostMapping
    @Operation(summary = "Submit new financial transaction",
               description = "Atomically validates account ownership, balance, creates transaction record, and queues outbox event")
    public ResponseEntity<ApiResponse<TransactionResponse>> submitTransaction(
            @Valid @RequestBody TransactionRequest request,
            @RequestHeader(value = TracingConstants.IDEMPOTENCY_KEY_HEADER, required = false) String idempotencyKey,
            @RequestHeader(value = TracingConstants.CORRELATION_ID_HEADER, required = false) String correlationId,
            @RequestHeader(value = TracingConstants.USER_ID_HEADER, required = false) String userId,
            @RequestHeader(value = TracingConstants.USER_ROLES_HEADER, required = false) String roles) {

        TransactionResponse response = transactionService.processTransaction(request, idempotencyKey, correlationId, userId, roles);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created("Transaction processed successfully", response));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get transaction by ID", description = "Enforces object ownership for USER; permitted for ANALYST and ADMIN")
    public ResponseEntity<ApiResponse<TransactionResponse>> getTransactionById(
            @PathVariable UUID id,
            @RequestHeader(value = TracingConstants.USER_ID_HEADER, required = false) String userId,
            @RequestHeader(value = TracingConstants.USER_ROLES_HEADER, required = false) String roles) {
        TransactionResponse response = transactionService.getTransactionById(id, userId, roles);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @GetMapping
    @Operation(summary = "Get transactions for account with pagination", description = "Enforces object ownership for USER; permitted for ANALYST and ADMIN")
    public ResponseEntity<ApiResponse<Page<TransactionResponse>>> getTransactionsByAccount(
            @RequestParam UUID accountId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestHeader(value = TracingConstants.USER_ID_HEADER, required = false) String userId,
            @RequestHeader(value = TracingConstants.USER_ROLES_HEADER, required = false) String roles) {

        Pageable pageable = PageRequest.of(page, size);
        Page<TransactionResponse> response = transactionService.getTransactionsByAccount(accountId, pageable, userId, roles);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @GetMapping("/accounts")
    @Operation(summary = "List accounts", description = "Returns only own accounts for USER; all accounts for ANALYST and ADMIN")
    public ResponseEntity<ApiResponse<List<AccountDto>>> getAllAccounts(
            @RequestHeader(value = TracingConstants.USER_ID_HEADER, required = false) String userId,
            @RequestHeader(value = TracingConstants.USER_ROLES_HEADER, required = false) String roles) {
        List<AccountDto> accounts = transactionService.getAllAccounts(userId, roles);
        return ResponseEntity.ok(ApiResponse.ok(accounts));
    }

    @GetMapping("/accounts/{accountId}")
    @Operation(summary = "Get account details and current balance", description = "Enforces object ownership for USER; permitted for ANALYST and ADMIN")
    public ResponseEntity<ApiResponse<AccountDto>> getAccountById(
            @PathVariable UUID accountId,
            @RequestHeader(value = TracingConstants.USER_ID_HEADER, required = false) String userId,
            @RequestHeader(value = TracingConstants.USER_ROLES_HEADER, required = false) String roles) {
        AccountDto account = transactionService.getAccountById(accountId, userId, roles);
        return ResponseEntity.ok(ApiResponse.ok(account));
    }
}

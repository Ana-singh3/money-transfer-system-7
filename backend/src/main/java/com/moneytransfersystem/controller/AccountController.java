package com.moneytransfersystem.controller;

import com.moneytransfersystem.domain.dtos.AccountHistoryItemDTO;
import com.moneytransfersystem.domain.dtos.AccountResponse;
import com.moneytransfersystem.domain.dtos.TransactionResponseDTO;
import com.moneytransfersystem.domain.dtos.UpdateAccountStatusRequest;
import com.moneytransfersystem.service.AccountService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/accounts")
@RequiredArgsConstructor
@Tag(name = "Accounts", description = "Account management endpoints")
@SecurityRequirement(name = "bearerAuth")
public class AccountController {

    private final AccountService accountService;

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    @Operation(summary = "Get account details", description = "Retrieves account information. Users can only access their own accounts, admins can access any account.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Account retrieved successfully"),
            @ApiResponse(responseCode = "403", description = "Unauthorized access to account"),
            @ApiResponse(responseCode = "404", description = "Account not found")
    })
    public ResponseEntity<AccountResponse> getAccount(@PathVariable String id) {
        AccountResponse response = accountService.getAccountById(id);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}/balance")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    @Operation(summary = "Get account balance", description = "Retrieves the balance of an account. Users can only access their own accounts, admins can access any account.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Balance retrieved successfully"),
            @ApiResponse(responseCode = "403", description = "Unauthorized access to account"),
            @ApiResponse(responseCode = "404", description = "Account not found")
    })
    public ResponseEntity<Map<String, Object>> getAccountBalance(@PathVariable String id) {
        BigDecimal balance = accountService.getAccountBalance(id);
        return ResponseEntity.ok(Map.of("accountId", id, "balance", balance));
    }

    @GetMapping("/{id}/transactions")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    @Operation(summary = "Get account transactions", description = "Retrieves all transactions for an account. Users can only access their own accounts, admins can access any account.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Transactions retrieved successfully"),
            @ApiResponse(responseCode = "403", description = "Unauthorized access to account"),
            @ApiResponse(responseCode = "404", description = "Account not found")
    })
    public ResponseEntity<List<TransactionResponseDTO>> getAccountTransactions(@PathVariable String id) {
        List<TransactionResponseDTO> transactions = accountService.getAccountTransactions(id);
        return ResponseEntity.ok(transactions);
    }

    @GetMapping("/{id}/history")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    @Operation(summary = "Get account history", description = "Transfers plus reward credit/debit entries.")
    public ResponseEntity<List<AccountHistoryItemDTO>> getAccountHistory(@PathVariable String id) {
        return ResponseEntity.ok(accountService.getAccountHistory(id));
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Activate or deactivate account (Admin only)")
    public ResponseEntity<AccountResponse> updateAccountStatus(
            @PathVariable String id,
            @Valid @RequestBody UpdateAccountStatusRequest request) {
        return ResponseEntity.ok(accountService.updateAccountStatus(id, request.getStatus()));
    }

    @GetMapping("/all")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Get all accounts (Admin only)", description = "Retrieves all accounts in the system. Admin access required.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Accounts retrieved successfully"),
            @ApiResponse(responseCode = "403", description = "Admin access required")
    })
    public ResponseEntity<List<AccountResponse>> getAllAccounts() {
        List<AccountResponse> accounts = accountService.getAllAccounts();
        return ResponseEntity.ok(accounts);
    }
}


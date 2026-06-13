package com.moneytransfersystem.service;

import com.moneytransfersystem.domain.dtos.AccountHistoryItemDTO;
import com.moneytransfersystem.domain.dtos.AccountResponse;
import com.moneytransfersystem.domain.dtos.TransactionResponseDTO;
import com.moneytransfersystem.domain.dtos.UpdateAccountStatusRequest;
import com.moneytransfersystem.domain.enums.AccountStatus;

import java.math.BigDecimal;
import java.util.List;

public interface AccountService {
    AccountResponse getAccountById(String accountId);
    BigDecimal getAccountBalance(String accountId);
    List<TransactionResponseDTO> getAccountTransactions(String accountId);
    List<AccountHistoryItemDTO> getAccountHistory(String accountId);
    List<AccountResponse> getAllAccounts();
    AccountResponse updateAccountStatus(String accountId, AccountStatus status);
}


package com.moneytransfersystem.service;

import com.moneytransfersystem.domain.dtos.AccountResponse;
import com.moneytransfersystem.domain.dtos.TransactionResponseDTO;

import java.math.BigDecimal;
import java.util.List;

public interface AccountService {
    AccountResponse getAccountById(String accountId);
    BigDecimal getAccountBalance(String accountId);
    List<TransactionResponseDTO> getAccountTransactions(String accountId);
    List<AccountResponse> getAllAccounts();
}


package com.moneytransfersystem.service;

import com.moneytransfersystem.domain.dtos.AccountResponse;
import com.moneytransfersystem.domain.entities.TransactionLog;

import java.math.BigDecimal;
import java.util.List;

public interface AccountService {
    AccountResponse getAccountById(String accountId);
    BigDecimal getAccountBalance(String accountId);
    List<TransactionLog> getAccountTransactions(String accountId);
    List<AccountResponse> getAllAccounts();
}


package com.moneytransfersystem.service;

import com.moneytransfersystem.domain.dtos.TransactionResponseDTO;

import java.util.List;

public interface ErrorLogService {
    List<TransactionResponseDTO> getFailedTransactions(String accountId);
}


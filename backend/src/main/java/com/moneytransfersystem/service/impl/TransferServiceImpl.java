package com.moneytransfersystem.service.impl;

import com.moneytransfersystem.domain.dtos.TransferRequest;
import com.moneytransfersystem.domain.dtos.TransferResponse;
import com.moneytransfersystem.domain.entities.Account;
import com.moneytransfersystem.domain.entities.TransactionLog;
import com.moneytransfersystem.domain.entities.User;
import com.moneytransfersystem.domain.enums.TransactionStatus;
import com.moneytransfersystem.domain.exceptions.AccountNotActiveException;
import com.moneytransfersystem.domain.exceptions.AccountNotFoundException;
import com.moneytransfersystem.domain.exceptions.DuplicateTransferException;
import com.moneytransfersystem.domain.exceptions.InsufficentBalanceException;
import com.moneytransfersystem.domain.exceptions.UnauthorizedAccessException;
import com.moneytransfersystem.repository.AccountRepository;
import com.moneytransfersystem.repository.TransactionLogRepository;
import com.moneytransfersystem.service.TransferService;
import com.moneytransfersystem.service.UserService;
import com.moneytransfersystem.util.IdGenerator;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
public class TransferServiceImpl implements TransferService {

    private final AccountRepository accountRepository;
    private final TransactionLogRepository transactionLogRepository;
    private final UserService userService;

    @Override
    @Transactional
    public TransferResponse transfer(TransferRequest request) {
        if (request.getFromAccountId().equals(request.getToAccountId())) {
            throw new IllegalArgumentException("Source and destination accounts cannot be the same");
        }

        transactionLogRepository.findByIdempotencyKey(request.getIdempotencyKey())
                .ifPresent(existing -> {
                    throw new DuplicateTransferException(request.getIdempotencyKey());
                });

        Account fromAccount = accountRepository.findByIdWithUser(request.getFromAccountId())
                .orElseThrow(() -> new AccountNotFoundException(request.getFromAccountId()));
        Account toAccount = accountRepository.findById(request.getToAccountId())
                .orElseThrow(() -> new AccountNotFoundException(request.getToAccountId()));

        User currentUser = userService.getCurrentUser();
        if (!userService.isAdmin(currentUser) && !fromAccount.getUser().getId().equals(currentUser.getId())) {
            throw new UnauthorizedAccessException(
                    "You do not have permission to transfer from account: " + request.getFromAccountId()
            );
        }

        if (!fromAccount.isActive()) {
            throw new AccountNotActiveException(request.getFromAccountId());
        }
        if (!toAccount.isActive()) {
            throw new AccountNotActiveException(request.getToAccountId());
        }

        BigDecimal transferAmount = request.getAmount().setScale(2, java.math.RoundingMode.HALF_UP);

        if (fromAccount.getBalance().compareTo(transferAmount) < 0) {
            throw new InsufficentBalanceException(
                    request.getFromAccountId(),
                    transferAmount,
                    fromAccount.getBalance()
            );
        }

        String transactionId = IdGenerator.generateTransactionId();

        try {
            fromAccount.debit(transferAmount);
            toAccount.credit(transferAmount);

            accountRepository.save(fromAccount);
            accountRepository.save(toAccount);

            TransactionLog transactionLog = new TransactionLog(
                    request.getFromAccountId(),
                    request.getToAccountId(),
                    transferAmount,
                    TransactionStatus.SUCCESS,
                    request.getIdempotencyKey()
            );
            transactionLog.setId(transactionId);
            transactionLogRepository.save(transactionLog);

            return new TransferResponse(
                    transactionId,
                    TransactionStatus.SUCCESS,
                    transferAmount,
                    "Transfer completed successfully"
            );

        } catch (Exception e) {
            TransactionLog transactionLog = new TransactionLog(
                    request.getFromAccountId(),
                    request.getToAccountId(),
                    transferAmount,
                    TransactionStatus.FAILED,
                    request.getIdempotencyKey()
            );
            transactionLog.setId(transactionId);
            transactionLog.setFailureReason(e.getMessage());
            transactionLogRepository.save(transactionLog);

            throw e;
        }
    }
}


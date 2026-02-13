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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
public class TransferServiceImpl implements TransferService {

    private static final Logger logger = LoggerFactory.getLogger(TransferServiceImpl.class);

    private final AccountRepository accountRepository;
    private final TransactionLogRepository transactionLogRepository;
    private final UserService userService;

    @Override
    @Transactional
    public TransferResponse transfer(TransferRequest request) {
        String transactionId = IdGenerator.generateTransactionId();

        logger.info("Transfer initiated | txId={} | from={} | to={} | amount={}",
                transactionId, request.getFromAccountId(),
                request.getToAccountId(), request.getAmount());

        validateSameAccount(request, transactionId);

        checkDuplicateTransfer(request);

        Account fromAccount = findFromAccount(request, transactionId);
        Account toAccount = findToAccount(request, transactionId);

        authorizeTransfer(request, fromAccount, transactionId);
        validateActiveAccounts(request, fromAccount, toAccount, transactionId);

        BigDecimal transferAmount = request.getAmount()
                .setScale(2, java.math.RoundingMode.HALF_UP);

        validateBalance(request, fromAccount, transferAmount, transactionId);

        return executeTransfer(request, fromAccount, toAccount, transferAmount, transactionId);
    }

    private void validateSameAccount(TransferRequest request, String txId) {
        if (request.getFromAccountId().equals(request.getToAccountId())) {
            logger.error("Transfer failed | txId={} | fromAccountId={} | toAccountId={} | amount={} | reason=SAME_ACCOUNT",
                    txId, request.getFromAccountId(), request.getToAccountId(), request.getAmount());
            throw new IllegalArgumentException(
                    "Source and destination accounts cannot be the same");
        }
    }

    private void checkDuplicateTransfer(TransferRequest request) {
        transactionLogRepository.findByIdempotencyKey(request.getIdempotencyKey())
                .ifPresent(existing -> {
                    logger.error("Transfer rejected | method=transfer | idempotencyKey={} | reason=DUPLICATE",
                            request.getIdempotencyKey());
                    throw new DuplicateTransferException(request.getIdempotencyKey());
                });
    }

    private Account findFromAccount(TransferRequest request, String txId) {
        return accountRepository.findByIdWithUser(request.getFromAccountId())
                .orElseThrow(() -> {
                    logger.error("Transfer failed | txId={} | fromAccountId={} | toAccountId={} | amount={} | reason=SENDER_NOT_FOUND",
                            txId, request.getFromAccountId(), request.getToAccountId(), request.getAmount());
                    return new AccountNotFoundException(request.getFromAccountId());
                });
    }

    private Account findToAccount(TransferRequest request, String txId) {
        return accountRepository.findById(request.getToAccountId())
                .orElseThrow(() -> {
                    logger.error("Transfer failed | txId={} | fromAccountId={} | toAccountId={} | amount={} | reason=RECEIVER_NOT_FOUND",
                            txId, request.getFromAccountId(), request.getToAccountId(), request.getAmount());
                    return new AccountNotFoundException(request.getToAccountId());
                });
    }

    private void authorizeTransfer(TransferRequest request, Account fromAccount, String txId) {
        User currentUser = userService.getCurrentUser();
        if (!userService.isAdmin(currentUser)
                && !fromAccount.getUser().getId().equals(currentUser.getId())) {
            logger.error("Transfer failed | txId={} | fromAccountId={} | toAccountId={} | amount={} | reason=UNAUTHORIZED",
                    txId, request.getFromAccountId(), request.getToAccountId(), request.getAmount());
            throw new UnauthorizedAccessException(
                    "You do not have permission to transfer from account: "
                            + request.getFromAccountId());
        }
    }

    private void validateActiveAccounts(TransferRequest request,
                                        Account fromAccount, Account toAccount, String txId) {
        if (!fromAccount.isActive()) {
            logger.error("Transfer failed | txId={} | fromAccountId={} | toAccountId={} | amount={} | reason=SENDER_NOT_ACTIVE",
                    txId, request.getFromAccountId(), request.getToAccountId(), request.getAmount());
            throw new AccountNotActiveException(request.getFromAccountId());
        }
        if (!toAccount.isActive()) {
            logger.error("Transfer failed | txId={} | fromAccountId={} | toAccountId={} | amount={} | reason=RECEIVER_NOT_ACTIVE",
                    txId, request.getFromAccountId(), request.getToAccountId(), request.getAmount());
            throw new AccountNotActiveException(request.getToAccountId());
        }
    }

    private void validateBalance(TransferRequest request, Account fromAccount,
                                 BigDecimal transferAmount, String txId) {
        if (fromAccount.getBalance().compareTo(transferAmount) < 0) {
            logger.error("Transfer failed | txId={} | fromAccountId={} | toAccountId={} | amount={} | reason=INSUFFICIENT_BALANCE",
                    txId, request.getFromAccountId(), request.getToAccountId(), transferAmount);
            throw new InsufficentBalanceException(
                    request.getFromAccountId(), transferAmount, fromAccount.getBalance());
        }
    }

    private TransferResponse executeTransfer(TransferRequest request,
                                             Account fromAccount, Account toAccount,
                                             BigDecimal transferAmount, String transactionId) {
        try {
            fromAccount.debit(transferAmount);
            toAccount.credit(transferAmount);

            accountRepository.save(fromAccount);
            accountRepository.save(toAccount);

            TransactionLog transactionLog = new TransactionLog(
                    request.getFromAccountId(), request.getToAccountId(),
                    transferAmount, TransactionStatus.SUCCESS,
                    request.getIdempotencyKey());
            transactionLog.setId(transactionId);
            transactionLogRepository.save(transactionLog);

            logger.info("Transfer success | txId={} | from={} | to={} | amount={}",
                    transactionId, request.getFromAccountId(),
                    request.getToAccountId(), transferAmount);

            return new TransferResponse(transactionId, TransactionStatus.SUCCESS,
                    transferAmount, "Transfer completed successfully");

        } catch (Exception e) {
            logger.error("Transfer failed | txId={} | fromAccountId={} | toAccountId={} | amount={} | reason={}",
                    transactionId, request.getFromAccountId(),
                    request.getToAccountId(), transferAmount, e.getMessage(), e);

            TransactionLog transactionLog = new TransactionLog(
                    request.getFromAccountId(), request.getToAccountId(),
                    transferAmount, TransactionStatus.FAILED,
                    request.getIdempotencyKey());
            transactionLog.setId(transactionId);
            transactionLog.setFailureReason(e.getMessage());
            transactionLogRepository.save(transactionLog);

            throw e;
        }
    }
}

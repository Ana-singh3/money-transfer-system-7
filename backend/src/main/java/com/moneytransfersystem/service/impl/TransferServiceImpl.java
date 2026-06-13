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
import com.moneytransfersystem.service.RewardService;
import com.moneytransfersystem.service.TransferService;
import com.moneytransfersystem.service.UserService;
import com.moneytransfersystem.util.IdGenerator;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
public class TransferServiceImpl implements TransferService {

    private static final Logger logger = LoggerFactory.getLogger(TransferServiceImpl.class);

    private final AccountRepository accountRepository;
    private final TransactionLogRepository transactionLogRepository;
    private final UserService userService;
    private final RewardService rewardService;
    private final PlatformTransactionManager transactionManager;

    @Override
    @Transactional
    public TransferResponse transfer(TransferRequest request) {
        String transactionId = IdGenerator.generateTransactionId();

        logger.info("Transfer initiated | txId={} | from={} | to={} | amount={}",
                transactionId, request.getFromAccountId(),
                request.getToAccountId(), request.getAmount());

        try {
            validateSameAccount(request, transactionId);
        } catch (IllegalArgumentException e) {
            BigDecimal amount = request.getAmount().setScale(2, java.math.RoundingMode.HALF_UP);
            persistFailedTransfer(request, amount, transactionId, "Source and destination accounts cannot be the same");
            throw e;
        }

        checkDuplicateTransfer(request);

        Account fromAccount;
        try {
            fromAccount = findFromAccount(request, transactionId);
        } catch (AccountNotFoundException e) {
            BigDecimal amount = request.getAmount().setScale(2, java.math.RoundingMode.HALF_UP);
            persistFailedTransfer(request, amount, transactionId, "Sender account not found");
            throw e;
        }

        Account toAccount;
        try {
            toAccount = findToAccount(request, transactionId);
        } catch (AccountNotFoundException e) {
            BigDecimal amount = request.getAmount().setScale(2, java.math.RoundingMode.HALF_UP);
            persistFailedTransfer(request, amount, transactionId, "Receiver account not found");
            throw e;
        }

        try {
            authorizeTransfer(request, fromAccount, transactionId);
        } catch (UnauthorizedAccessException e) {
            BigDecimal amount = request.getAmount().setScale(2, java.math.RoundingMode.HALF_UP);
            persistFailedTransfer(request, amount, transactionId, "Unauthorized access");
            throw e;
        }
        
        try {
            validateActiveAccounts(request, fromAccount, toAccount, transactionId);
        } catch (AccountNotActiveException e) {
            // Already persisted in validateActiveAccounts
            throw e;
        }

        BigDecimal transferAmount = request.getAmount()
                .setScale(2, java.math.RoundingMode.HALF_UP);

        User sender = fromAccount.getUser();
        int rewardPointsUsed = request.getRewardPointsToUse() == null ? 0 : request.getRewardPointsToUse();
        BigDecimal cashAmount = rewardService.resolveCashAmount(sender, transferAmount, rewardPointsUsed);

        try {
            validateBalance(request, fromAccount, cashAmount, transactionId);
        } catch (InsufficentBalanceException e) {
            // Already persisted in validateBalance
            throw e;
        }

        return executeTransfer(request, fromAccount, toAccount, transferAmount, cashAmount, rewardPointsUsed, transactionId);
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
        return accountRepository.findByIdWithUser(request.getToAccountId())
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
        BigDecimal amount = request.getAmount().setScale(2, java.math.RoundingMode.HALF_UP);
        if (!fromAccount.isActive()) {
            logger.error("Transfer failed | txId={} | reason=SENDER_NOT_ACTIVE", txId);
            persistFailedTransfer(request, amount, txId, "Sender account is deactivated");
            throw new AccountNotActiveException(request.getFromAccountId());
        }
        if (!toAccount.isActive()) {
            logger.error("Transfer failed | txId={} | reason=RECEIVER_NOT_ACTIVE", txId);
            persistFailedTransfer(request, amount, txId, "Receiver account is deactivated");
            throw new AccountNotActiveException(request.getToAccountId());
        }
    }

    private void persistFailedTransfer(TransferRequest request, BigDecimal amount,
                                       String transactionId, String reason) {
        TransactionTemplate txTemplate = new TransactionTemplate(transactionManager);
        txTemplate.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        txTemplate.execute(status -> {
            TransactionLog failedLog = new TransactionLog(
                    request.getFromAccountId(), request.getToAccountId(),
                    amount, TransactionStatus.FAILED, request.getIdempotencyKey());
            failedLog.setId(transactionId);
            failedLog.setFailureReason(reason);
            transactionLogRepository.save(failedLog);
            logger.info("Failed transaction persisted | txId={} | reason={}", transactionId, reason);
            return null;
        });
    }

    private void validateBalance(TransferRequest request, Account fromAccount,
                                 BigDecimal cashRequired, String txId) {
        if (cashRequired.compareTo(BigDecimal.ZERO) > 0
                && fromAccount.getBalance().compareTo(cashRequired) < 0) {
            logger.error("Transfer failed | txId={} | fromAccountId={} | toAccountId={} | cashRequired={} | reason=INSUFFICIENT_BALANCE",
                    txId, request.getFromAccountId(), request.getToAccountId(), cashRequired);
            BigDecimal amount = request.getAmount().setScale(2, java.math.RoundingMode.HALF_UP);
            persistFailedTransfer(request, amount, txId, "Insufficient balance");
            throw new InsufficentBalanceException(
                    request.getFromAccountId(), cashRequired, fromAccount.getBalance());
        }
    }

    private TransferResponse executeTransfer(TransferRequest request,
                                             Account fromAccount, Account toAccount,
                                             BigDecimal transferAmount, BigDecimal cashAmount,
                                             int rewardPointsUsed, String transactionId) {
        try {
            if (cashAmount.compareTo(BigDecimal.ZERO) > 0) {
                fromAccount.debit(cashAmount);
            }
            toAccount.credit(transferAmount);

            accountRepository.save(fromAccount);
            accountRepository.save(toAccount);

            TransactionLog transactionLog = new TransactionLog(
                    request.getFromAccountId(), request.getToAccountId(),
                    transferAmount, TransactionStatus.SUCCESS,
                    request.getIdempotencyKey());
            transactionLog.setId(transactionId);
            transactionLogRepository.save(transactionLog);

            if (rewardPointsUsed > 0) {
                rewardService.redeemPointsForTransfer(fromAccount.getUser(), transactionId, rewardPointsUsed);
            }
            rewardService.processRewardForTransfer(transactionLog, fromAccount, toAccount);

            logger.info("Transfer success | txId={} | from={} | to={} | amount={} | cash={} | rewardPoints={}",
                    transactionId, request.getFromAccountId(),
                    request.getToAccountId(), transferAmount, cashAmount, rewardPointsUsed);

            TransferResponse response = new TransferResponse(transactionId, TransactionStatus.SUCCESS,
                    transferAmount, "Transfer completed successfully");
            response.setCashAmountPaid(cashAmount);
            response.setRewardPointsUsed(rewardPointsUsed);
            return response;

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

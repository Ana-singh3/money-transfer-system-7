package com.moneytransfersystem.service.impl;

import com.moneytransfersystem.domain.dtos.AccountResponse;
import com.moneytransfersystem.domain.dtos.TransactionResponseDTO;
import com.moneytransfersystem.domain.entities.Account;
import com.moneytransfersystem.domain.entities.TransactionLog;
import com.moneytransfersystem.domain.entities.User;
import com.moneytransfersystem.domain.exceptions.AccountNotFoundException;
import com.moneytransfersystem.domain.exceptions.UnauthorizedAccessException;
import com.moneytransfersystem.repository.AccountRepository;
import com.moneytransfersystem.repository.TransactionLogRepository;
import com.moneytransfersystem.service.AccountService;
import com.moneytransfersystem.service.ErrorLogService;
import com.moneytransfersystem.service.UserService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AccountServiceImpl implements AccountService {

    private static final Logger logger = LoggerFactory.getLogger(AccountServiceImpl.class);

    private final AccountRepository accountRepository;
    private final TransactionLogRepository transactionLogRepository;
    private final UserService userService;
    private final ErrorLogService errorLogService;

    private void checkAccountAccess(Account account) {
        User currentUser = userService.getCurrentUser();
        if (!userService.isAdmin(currentUser) && !account.getUser().getId().equals(currentUser.getId())) {
            logger.error("Access denied | method=checkAccountAccess | accountId={} | userId={} | reason=UNAUTHORIZED",
                    account.getId(), currentUser.getId());
            throw new UnauthorizedAccessException(
                    "You do not have permission to access account: " + account.getId()
            );
        }
    }

    @Override
    public AccountResponse getAccountById(String accountId) {
        logger.info("Fetching account | method=getAccountById | accountId={}", accountId);

        Account account = accountRepository.findByIdWithUser(accountId)
                .orElseThrow(() -> {
                    logger.error("Account not found | method=getAccountById | accountId={}", accountId);
                    return new AccountNotFoundException(accountId);
                });

        checkAccountAccess(account);

        AccountResponse response = new AccountResponse();
        response.setAccountId(account.getId());
        response.setHolderName(account.getHolderName());
        response.setBalance(account.getBalance());
        response.setStatus(account.getStatus().name());

        return response;
    }

    @Override
    public BigDecimal getAccountBalance(String accountId) {
        logger.info("Fetching balance | method=getAccountBalance | accountId={}", accountId);

        Account account = accountRepository.findByIdWithUser(accountId)
                .orElseThrow(() -> {
                    logger.error("Account not found | method=getAccountBalance | accountId={}", accountId);
                    return new AccountNotFoundException(accountId);
                });

        checkAccountAccess(account);

        return account.getBalance();
    }

    @Override
    public List<TransactionResponseDTO> getAccountTransactions(String accountId) {
        logger.info("Fetching transactions | method=getAccountTransactions | accountId={}", accountId);

        Account account = accountRepository.findByIdWithUser(accountId)
                .orElseThrow(() -> {
                    logger.error("Account not found | method=getAccountTransactions | accountId={}", accountId);
                    return new AccountNotFoundException(accountId);
                });

        checkAccountAccess(account);

        List<TransactionLog> dbTransactions = transactionLogRepository
                .findByFromAccountIdOrToAccountIdOrderByCreatedOnDesc(accountId, accountId);

        List<TransactionResponseDTO> result = new ArrayList<>(dbTransactions.stream()
                .map(this::toDTO)
                .toList());

        Set<String> dbTxIds = dbTransactions.stream()
                .map(TransactionLog::getId)
                .collect(Collectors.toSet());

        List<TransactionResponseDTO> logFailures = errorLogService.getFailedTransactions(accountId);
        for (TransactionResponseDTO failed : logFailures) {
            if (!dbTxIds.contains(failed.getId())) {
                result.add(failed);
            }
        }

        result.sort(Comparator.comparing(TransactionResponseDTO::getCreatedOn).reversed());
        return result;
    }

    private TransactionResponseDTO toDTO(TransactionLog log) {
        return new TransactionResponseDTO(
                log.getId(),
                log.getFromAccountId(),
                log.getToAccountId(),
                log.getAmount(),
                log.getStatus().name(),
                log.getFailureReason(),
                log.getCreatedOn()
        );
    }

    @Override
    public List<AccountResponse> getAllAccounts() {
        logger.info("Fetching all accounts | method=getAllAccounts");

        List<Account> accounts = accountRepository.findAll();

        logger.info("Retrieved {} accounts | method=getAllAccounts", accounts.size());

        return accounts.stream()
                .map(account -> {
                    AccountResponse response = new AccountResponse();
                    response.setAccountId(account.getId());
                    response.setHolderName(account.getHolderName());
                    response.setBalance(account.getBalance());
                    response.setStatus(account.getStatus().name());
                    return response;
                })
                .toList();
    }
}

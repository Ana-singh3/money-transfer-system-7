package com.moneytransfersystem.service.impl;

import com.moneytransfersystem.domain.dtos.AccountResponse;
import com.moneytransfersystem.domain.entities.Account;
import com.moneytransfersystem.domain.entities.TransactionLog;
import com.moneytransfersystem.domain.entities.User;
import com.moneytransfersystem.domain.exceptions.AccountNotFoundException;
import com.moneytransfersystem.domain.exceptions.UnauthorizedAccessException;
import com.moneytransfersystem.repository.AccountRepository;
import com.moneytransfersystem.repository.TransactionLogRepository;
import com.moneytransfersystem.service.AccountService;
import com.moneytransfersystem.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AccountServiceImpl implements AccountService {

    private final AccountRepository accountRepository;
    private final TransactionLogRepository transactionLogRepository;
    private final UserService userService;

    private void checkAccountAccess(Account account) {
        User currentUser = userService.getCurrentUser();
        if (!userService.isAdmin(currentUser) && !account.getUser().getId().equals(currentUser.getId())) {
            throw new UnauthorizedAccessException(
                    "You do not have permission to access account: " + account.getId()
            );
        }
    }

    @Override
    public AccountResponse getAccountById(String accountId) {
        Account account = accountRepository.findByIdWithUser(accountId)
                .orElseThrow(() -> new AccountNotFoundException(accountId));

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
        Account account = accountRepository.findByIdWithUser(accountId)
                .orElseThrow(() -> new AccountNotFoundException(accountId));
        
        checkAccountAccess(account);
        
        return account.getBalance();
    }

    @Override
    public List<TransactionLog> getAccountTransactions(String accountId) {
        Account account = accountRepository.findByIdWithUser(accountId)
                .orElseThrow(() -> new AccountNotFoundException(accountId));
        
        checkAccountAccess(account);
        
        return transactionLogRepository.findByFromAccountIdOrToAccountIdOrderByCreatedOnDesc(
                accountId, accountId);
    }

    @Override
    public List<AccountResponse> getAllAccounts() {
        List<Account> accounts = accountRepository.findAll();
        
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


package com.moneytransfersystem.service.impl;

import com.moneytransfersystem.domain.dtos.AccountHistoryItemDTO;
import com.moneytransfersystem.domain.dtos.AccountResponse;
import com.moneytransfersystem.domain.dtos.TransactionResponseDTO;
import com.moneytransfersystem.domain.entities.Account;
import com.moneytransfersystem.domain.entities.RewardGrant;
import com.moneytransfersystem.domain.entities.RewardRedemption;
import com.moneytransfersystem.domain.entities.TransactionLog;
import com.moneytransfersystem.domain.entities.User;
import com.moneytransfersystem.domain.enums.AccountStatus;
import com.moneytransfersystem.domain.exceptions.AccountNotFoundException;
import com.moneytransfersystem.domain.exceptions.UnauthorizedAccessException;
import com.moneytransfersystem.repository.AccountRepository;
import com.moneytransfersystem.repository.RewardGrantRepository;
import com.moneytransfersystem.repository.RewardRedemptionRepository;
import com.moneytransfersystem.repository.TransactionLogRepository;
import com.moneytransfersystem.service.AccountService;
import com.moneytransfersystem.service.ErrorLogService;
import com.moneytransfersystem.service.RewardService;
import com.moneytransfersystem.service.UserService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
    private final RewardGrantRepository rewardGrantRepository;
    private final RewardRedemptionRepository rewardRedemptionRepository;
    private final UserService userService;
    private final RewardService rewardService;
    private final ErrorLogService errorLogService;

    private void checkAccountAccess(Account account) {
        User currentUser = userService.getCurrentUser();
        if (!userService.isAdmin(currentUser) && !account.getUser().getId().equals(currentUser.getId())) {
            throw new UnauthorizedAccessException(
                    "You do not have permission to access account: " + account.getId());
        }
    }

    private void requireAdmin() {
        if (!userService.isAdmin()) {
            throw new UnauthorizedAccessException("Admin access required");
        }
    }

    private Account loadAccountWithAccess(String accountId) {
        Account account = accountRepository.findByIdWithUser(accountId)
                .orElseThrow(() -> new AccountNotFoundException(accountId));
        checkAccountAccess(account);
        return account;
    }

    @Override
    public AccountResponse getAccountById(String accountId) {
        Account account = loadAccountWithAccess(accountId);
        return toResponse(account);
    }

    @Override
    public BigDecimal getAccountBalance(String accountId) {
        return loadAccountWithAccess(accountId).getBalance();
    }

    @Override
    public List<TransactionResponseDTO> getAccountTransactions(String accountId) {
        loadAccountWithAccess(accountId);
        return buildTransferList(accountId);
    }

    @Override
    public List<AccountHistoryItemDTO> getAccountHistory(String accountId) {
        Account account = loadAccountWithAccess(accountId);
        List<AccountHistoryItemDTO> history = new ArrayList<>();

        for (TransactionResponseDTO tx : buildTransferList(accountId)) {
            history.add(new AccountHistoryItemDTO(
                    "TRANSFER", tx.getId(), tx.getCreatedOn(),
                    tx.getFromAccountId(), tx.getToAccountId(), tx.getAmount(),
                    tx.getStatus(), tx.getFailureReason(), null, null));
        }

        Long userId = account.getUser().getId();
        for (RewardGrant grant : rewardGrantRepository.findByUser_IdOrderByCreatedOnDesc(userId)) {
            history.add(new AccountHistoryItemDTO(
                    "REWARD_CREDIT", grant.getId(), grant.getCreatedOn(),
                    null, null, grant.getTransactionAmount(),
                    "SUCCESS", null, grant.getPoints(), grant.getTransactionId()));
        }
        for (RewardRedemption redemption : rewardRedemptionRepository.findByUser_IdOrderByCreatedOnDesc(userId)) {
            history.add(new AccountHistoryItemDTO(
                    "REWARD_DEBIT", redemption.getId(), redemption.getCreatedOn(),
                    null, null, redemption.getRupeeValue(),
                    "SUCCESS", null, -redemption.getPointsUsed(), redemption.getTransactionId()));
        }

        history.sort(Comparator.comparing(AccountHistoryItemDTO::getCreatedOn).reversed());
        return history;
    }

    @Override
    public List<AccountResponse> getAllAccounts() {
        requireAdmin();
        return accountRepository.findAll().stream().map(this::toResponse).toList();
    }

    @Override
    @Transactional
    public AccountResponse updateAccountStatus(String accountId, AccountStatus status) {
        requireAdmin();
        if (status != AccountStatus.ACTIVE && status != AccountStatus.LOCKED) {
            throw new IllegalArgumentException("Only ACTIVE or LOCKED status is allowed for toggle");
        }
        Account account = accountRepository.findByIdWithUser(accountId)
                .orElseThrow(() -> new AccountNotFoundException(accountId));
        account.setStatus(status);
        accountRepository.save(account);
        logger.info("Account status updated | accountId={} | status={}", accountId, status);
        return toResponse(account);
    }

    private List<TransactionResponseDTO> buildTransferList(String accountId) {
        List<TransactionLog> dbTransactions = transactionLogRepository
                .findByFromAccountIdOrToAccountIdOrderByCreatedOnDesc(accountId, accountId);

        List<TransactionResponseDTO> result = new ArrayList<>(dbTransactions.stream()
                .map(this::toDTO).toList());

        Set<String> dbTxIds = dbTransactions.stream().map(TransactionLog::getId).collect(Collectors.toSet());
        for (TransactionResponseDTO failed : errorLogService.getFailedTransactions(accountId)) {
            if (!dbTxIds.contains(failed.getId())) {
                result.add(failed);
            }
        }
        result.sort(Comparator.comparing(TransactionResponseDTO::getCreatedOn).reversed());
        return result;
    }

    private AccountResponse toResponse(Account account) {
        AccountResponse response = new AccountResponse();
        response.setAccountId(account.getId());
        response.setHolderName(account.getHolderName());
        response.setBalance(account.getBalance());
        response.setStatus(account.getStatus().name());
        response.setAvailableRewardPoints(rewardService.getAvailablePoints(account.getUser().getId()));
        return response;
    }

    private TransactionResponseDTO toDTO(TransactionLog log) {
        return new TransactionResponseDTO(
                log.getId(), log.getFromAccountId(), log.getToAccountId(),
                log.getAmount(), log.getStatus().name(), log.getFailureReason(), log.getCreatedOn());
    }
}

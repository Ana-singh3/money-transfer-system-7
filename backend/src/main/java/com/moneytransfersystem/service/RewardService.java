package com.moneytransfersystem.service;

import com.moneytransfersystem.domain.dtos.RewardSummaryResponse;
import com.moneytransfersystem.domain.entities.Account;
import com.moneytransfersystem.domain.entities.TransactionLog;
import com.moneytransfersystem.domain.entities.User;

import java.math.BigDecimal;

public interface RewardService {

    void processRewardForTransfer(TransactionLog transaction, Account fromAccount, Account toAccount);

    void redeemPointsForTransfer(User user, String transactionId, int pointsToUse);

    int getAvailablePoints(Long userId);

    /** Validates reward redemption; returns cash portion still required from account balance. */
    BigDecimal resolveCashAmount(User user, BigDecimal transferAmount, Integer rewardPointsToUse);

    RewardSummaryResponse getMyRewards();
}

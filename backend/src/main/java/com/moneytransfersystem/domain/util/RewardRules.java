package com.moneytransfersystem.domain.util;

import com.moneytransfersystem.domain.entities.Account;
import com.moneytransfersystem.domain.entities.TransactionLog;
import com.moneytransfersystem.domain.enums.TransactionStatus;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Pure reward eligibility and calculation rules.
 * 1 point per ₹100 transferred (rounded down) when all eligibility checks pass.
 */
public final class RewardRules {

    /** Minimum transfer amount (exclusive) for reward eligibility. */
    public static final BigDecimal MIN_ELIGIBLE_AMOUNT = new BigDecimal("100.00");

    private RewardRules() {
    }

    /**
     * A transaction earns rewards only when:
     * - status is SUCCESS
     * - amount is strictly greater than ₹100
     * - sender and receiver belong to different users
     * - accounts are not the same (not a self-transfer)
     */
    public static boolean isEligible(TransactionLog transaction, Account fromAccount, Account toAccount) {
        if (transaction.getStatus() != TransactionStatus.SUCCESS) {
            return false;
        }
        if (transaction.getAmount().compareTo(MIN_ELIGIBLE_AMOUNT) <= 0) {
            return false;
        }
        if (transaction.getFromAccountId().equals(transaction.getToAccountId())) {
            return false;
        }
        Long senderUserId = fromAccount.getUser().getId();
        Long receiverUserId = toAccount.getUser().getId();
        return !senderUserId.equals(receiverUserId);
    }

    /** floor(amount / 100) when eligible; otherwise 0. */
    public static int calculatePoints(BigDecimal amount, boolean eligible) {
        if (!eligible) {
            return 0;
        }
        return amount.divide(MIN_ELIGIBLE_AMOUNT, 0, RoundingMode.FLOOR).intValue();
    }
}

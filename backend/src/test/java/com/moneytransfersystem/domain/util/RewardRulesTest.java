package com.moneytransfersystem.domain.util;

import com.moneytransfersystem.domain.entities.Account;
import com.moneytransfersystem.domain.entities.TransactionLog;
import com.moneytransfersystem.domain.entities.User;
import com.moneytransfersystem.domain.enums.AccountStatus;
import com.moneytransfersystem.domain.enums.TransactionStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("RewardRules")
class RewardRulesTest {

    private User sender;
    private User receiver;
    private Account fromAccount;
    private Account toAccount;
    private TransactionLog transaction;

    @BeforeEach
    void setUp() {
        sender = new User();
        sender.setId(1L);

        receiver = new User();
        receiver.setId(2L);

        fromAccount = new Account("ACC-FROM", "Sender", new BigDecimal("1000.00"), AccountStatus.ACTIVE);
        fromAccount.setUser(sender);

        toAccount = new Account("ACC-TO", "Receiver", new BigDecimal("500.00"), AccountStatus.ACTIVE);
        toAccount.setUser(receiver);

        transaction = new TransactionLog("ACC-FROM", "ACC-TO",
                new BigDecimal("250.00"), TransactionStatus.SUCCESS, "KEY-1");
    }

    @Nested
    @DisplayName("Eligibility")
    class Eligibility {

        @Test
        @DisplayName("eligible when all conditions are met")
        void eligibleTransfer() {
            assertThat(RewardRules.isEligible(transaction, fromAccount, toAccount)).isTrue();
        }

        @Test
        @DisplayName("not eligible when status is FAILED")
        void failedStatus() {
            transaction.setStatus(TransactionStatus.FAILED);
            assertThat(RewardRules.isEligible(transaction, fromAccount, toAccount)).isFalse();
        }

        @Test
        @DisplayName("not eligible when amount is exactly ₹100")
        void amountExactly100() {
            transaction.setAmount(new BigDecimal("100.00"));
            assertThat(RewardRules.isEligible(transaction, fromAccount, toAccount)).isFalse();
        }

        @Test
        @DisplayName("not eligible when amount is below ₹100")
        void amountBelow100() {
            transaction.setAmount(new BigDecimal("99.00"));
            assertThat(RewardRules.isEligible(transaction, fromAccount, toAccount)).isFalse();
        }

        @Test
        @DisplayName("not eligible for self-transfer (same account)")
        void sameAccount() {
            transaction.setToAccountId("ACC-FROM");
            assertThat(RewardRules.isEligible(transaction, fromAccount, toAccount)).isFalse();
        }

        @Test
        @DisplayName("not eligible when sender and receiver are the same user")
        void sameUser() {
            toAccount.setUser(sender);
            assertThat(RewardRules.isEligible(transaction, fromAccount, toAccount)).isFalse();
        }
    }

    @Nested
    @DisplayName("Point calculation")
    class PointCalculation {

        @Test
        @DisplayName("₹250 yields 2 points when eligible")
        void twoHundredFifty() {
            assertThat(RewardRules.calculatePoints(new BigDecimal("250.00"), true)).isEqualTo(2);
        }

        @Test
        @DisplayName("₹199 yields 1 point when eligible")
        void oneNinetyNine() {
            assertThat(RewardRules.calculatePoints(new BigDecimal("199.00"), true)).isEqualTo(1);
        }

        @Test
        @DisplayName("₹99 yields 0 points (not eligible)")
        void ninetyNine() {
            assertThat(RewardRules.calculatePoints(new BigDecimal("99.00"), false)).isZero();
        }

        @Test
        @DisplayName("returns 0 when not eligible regardless of amount")
        void ineligibleReturnsZero() {
            assertThat(RewardRules.calculatePoints(new BigDecimal("500.00"), false)).isZero();
        }
    }
}

package com.moneytransfersystem.service.impl;

import com.moneytransfersystem.domain.dtos.RewardSummaryResponse;
import com.moneytransfersystem.domain.entities.Account;
import com.moneytransfersystem.domain.entities.RewardGrant;
import com.moneytransfersystem.domain.entities.RewardRedemption;
import com.moneytransfersystem.domain.entities.TransactionLog;
import com.moneytransfersystem.domain.entities.User;
import com.moneytransfersystem.domain.enums.AccountStatus;
import com.moneytransfersystem.domain.enums.Role;
import com.moneytransfersystem.domain.enums.TransactionStatus;
import com.moneytransfersystem.domain.exceptions.InsufficientRewardPointsException;
import com.moneytransfersystem.repository.RewardGrantRepository;
import com.moneytransfersystem.repository.RewardRedemptionRepository;
import com.moneytransfersystem.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("RewardServiceImpl")
class RewardServiceImplTest {

    @Mock private RewardGrantRepository rewardGrantRepository;
    @Mock private RewardRedemptionRepository rewardRedemptionRepository;
    @Mock private UserService userService;
    @InjectMocks private RewardServiceImpl rewardService;

    private User sender;
    private User receiver;
    private Account fromAccount;
    private Account toAccount;

    @BeforeEach
    void setUp() {
        sender = new User();
        sender.setId(1L);
        sender.setUsername("sender");
        sender.setRole(Role.ROLE_USER);

        receiver = new User();
        receiver.setId(2L);

        fromAccount = new Account("ACC-FROM", "Sender", new BigDecimal("1000.00"), AccountStatus.ACTIVE);
        fromAccount.setUser(sender);

        toAccount = new Account("ACC-TO", "Receiver", new BigDecimal("500.00"), AccountStatus.ACTIVE);
        toAccount.setUser(receiver);
    }

    @Nested
    @DisplayName("processRewardForTransfer")
    class ProcessReward {

        @Test
        @DisplayName("grants points for eligible transfer")
        void grantsPoints() {
            TransactionLog tx = new TransactionLog("ACC-FROM", "ACC-TO",
                    new BigDecimal("250.00"), TransactionStatus.SUCCESS, "KEY-1");
            tx.setId("TXN-1");

            when(rewardGrantRepository.findByTransactionId("TXN-1")).thenReturn(Optional.empty());
            when(rewardGrantRepository.save(any())).thenAnswer(i -> i.getArgument(0));

            rewardService.processRewardForTransfer(tx, fromAccount, toAccount);

            ArgumentCaptor<RewardGrant> cap = ArgumentCaptor.forClass(RewardGrant.class);
            verify(rewardGrantRepository).save(cap.capture());
            assertThat(cap.getValue().getPoints()).isEqualTo(2);
        }
    }

    @Nested
    @DisplayName("redeemPointsForTransfer")
    class RedeemPoints {

        @Test
        @DisplayName("records redemption when points are used")
        void recordsRedemption() {
            when(rewardRedemptionRepository.findByTransactionId("TXN-1")).thenReturn(Optional.empty());
            when(rewardRedemptionRepository.save(any())).thenAnswer(i -> i.getArgument(0));

            rewardService.redeemPointsForTransfer(sender, "TXN-1", 50);

            ArgumentCaptor<RewardRedemption> cap = ArgumentCaptor.forClass(RewardRedemption.class);
            verify(rewardRedemptionRepository).save(cap.capture());
            assertThat(cap.getValue().getPointsUsed()).isEqualTo(50);
            assertThat(cap.getValue().getId()).startsWith("RDM-");
        }
    }

    @Nested
    @DisplayName("resolveCashAmount")
    class ResolveCash {

        @Test
        @DisplayName("returns full amount when no points used")
        void noPoints() {
            BigDecimal cash = rewardService.resolveCashAmount(sender, new BigDecimal("200.00"), 0);
            assertThat(cash).isEqualByComparingTo("200.00");
        }

        @Test
        @DisplayName("subtracts reward value from transfer amount")
        void partialPayment() {
            when(rewardGrantRepository.sumPointsByUserId(1L)).thenReturn(100);
            when(rewardRedemptionRepository.sumPointsUsedByUserId(1L)).thenReturn(20);

            BigDecimal cash = rewardService.resolveCashAmount(sender, new BigDecimal("150.00"), 50);
            assertThat(cash).isEqualByComparingTo("100.00");
        }

        @Test
        @DisplayName("throws when insufficient reward points")
        void insufficientPoints() {
            when(rewardGrantRepository.sumPointsByUserId(1L)).thenReturn(10);
            when(rewardRedemptionRepository.sumPointsUsedByUserId(1L)).thenReturn(0);

            assertThatThrownBy(() -> rewardService.resolveCashAmount(sender, new BigDecimal("200.00"), 50))
                    .isInstanceOf(InsufficientRewardPointsException.class);
        }
    }

    @Nested
    @DisplayName("getMyRewards")
    class GetMyRewards {

        @Test
        @DisplayName("returns available balance and histories")
        void returnsSummary() {
            when(userService.getCurrentUser()).thenReturn(sender);
            when(rewardGrantRepository.sumPointsByUserId(1L)).thenReturn(10);
            when(rewardRedemptionRepository.sumPointsUsedByUserId(1L)).thenReturn(3);

            RewardGrant grant = new RewardGrant(sender, "TXN-1", 5, new BigDecimal("500.00"));
            grant.setId("RWD-1");
            grant.setCreatedOn(Instant.parse("2026-06-01T10:00:00Z"));

            RewardRedemption redemption = new RewardRedemption(sender, "TXN-2", 3);
            redemption.setId("RDM-1");
            redemption.setCreatedOn(Instant.parse("2026-06-02T10:00:00Z"));

            when(rewardGrantRepository.findByUser_IdOrderByCreatedOnDesc(1L)).thenReturn(List.of(grant));
            when(rewardRedemptionRepository.findByUser_IdOrderByCreatedOnDesc(1L)).thenReturn(List.of(redemption));

            RewardSummaryResponse summary = rewardService.getMyRewards();

            assertThat(summary.getAvailablePoints()).isEqualTo(7);
            assertThat(summary.getTotalEarned()).isEqualTo(10);
            assertThat(summary.getTotalRedeemed()).isEqualTo(3);
            assertThat(summary.getHistory()).hasSize(1);
            assertThat(summary.getRedemptions()).hasSize(1);
        }
    }
}

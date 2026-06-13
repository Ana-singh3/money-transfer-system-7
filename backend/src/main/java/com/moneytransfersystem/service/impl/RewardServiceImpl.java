package com.moneytransfersystem.service.impl;

import com.moneytransfersystem.domain.dtos.RewardRedemptionResponse;
import com.moneytransfersystem.domain.dtos.RewardResponse;
import com.moneytransfersystem.domain.dtos.RewardSummaryResponse;
import com.moneytransfersystem.domain.entities.Account;
import com.moneytransfersystem.domain.entities.RewardGrant;
import com.moneytransfersystem.domain.entities.RewardRedemption;
import com.moneytransfersystem.domain.entities.TransactionLog;
import com.moneytransfersystem.domain.entities.User;
import com.moneytransfersystem.domain.exceptions.InsufficientRewardPointsException;
import com.moneytransfersystem.domain.util.RewardRules;
import com.moneytransfersystem.repository.RewardGrantRepository;
import com.moneytransfersystem.repository.RewardRedemptionRepository;
import com.moneytransfersystem.service.RewardService;
import com.moneytransfersystem.service.UserService;
import com.moneytransfersystem.util.IdGenerator;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
public class RewardServiceImpl implements RewardService {

    private static final Logger logger = LoggerFactory.getLogger(RewardServiceImpl.class);

    private final RewardGrantRepository rewardGrantRepository;
    private final RewardRedemptionRepository rewardRedemptionRepository;
    private final UserService userService;

    @Override
    public void processRewardForTransfer(TransactionLog transaction,
                                         Account fromAccount, Account toAccount) {
        if (rewardGrantRepository.findByTransactionId(transaction.getId()).isPresent()) {
            logger.warn("Reward already processed | txId={}", transaction.getId());
            return;
        }

        boolean eligible = RewardRules.isEligible(transaction, fromAccount, toAccount);
        int points = RewardRules.calculatePoints(transaction.getAmount(), eligible);

        if (points <= 0) {
            logger.info("No reward granted | txId={} | eligible={} | amount={}",
                    transaction.getId(), eligible, transaction.getAmount());
            return;
        }

        User sender = fromAccount.getUser();
        RewardGrant grant = new RewardGrant(
                sender, transaction.getId(), points, transaction.getAmount());
        grant.setId(IdGenerator.generateRewardId());
        rewardGrantRepository.save(grant);

        logger.info("Reward granted | rewardId={} | userId={} | txId={} | points={}",
                grant.getId(), sender.getId(), transaction.getId(), points);
    }

    @Override
    public void redeemPointsForTransfer(User user, String transactionId, int pointsToUse) {
        if (pointsToUse <= 0) {
            return;
        }
        if (rewardRedemptionRepository.findByTransactionId(transactionId).isPresent()) {
            logger.warn("Redemption already processed | txId={}", transactionId);
            return;
        }

        RewardRedemption redemption = new RewardRedemption(user, transactionId, pointsToUse);
        redemption.setId(IdGenerator.generateRedemptionId());
        rewardRedemptionRepository.save(redemption);

        logger.info("Reward redeemed | redemptionId={} | userId={} | txId={} | points={}",
                redemption.getId(), user.getId(), transactionId, pointsToUse);
    }

    @Override
    public int getAvailablePoints(Long userId) {
        int earned = rewardGrantRepository.sumPointsByUserId(userId);
        int redeemed = rewardRedemptionRepository.sumPointsUsedByUserId(userId);
        return Math.max(0, earned - redeemed);
    }

    @Override
    public BigDecimal resolveCashAmount(User user, BigDecimal transferAmount, Integer rewardPointsToUse) {
        int points = rewardPointsToUse == null ? 0 : rewardPointsToUse;
        if (points < 0) {
            throw new IllegalArgumentException("Reward points to use cannot be negative");
        }
        if (points == 0) {
            return transferAmount;
        }

        BigDecimal rewardValue = BigDecimal.valueOf(points).setScale(2, java.math.RoundingMode.HALF_UP);
        if (rewardValue.compareTo(transferAmount) > 0) {
            throw new IllegalArgumentException(
                    "Reward points cannot exceed transfer amount (1 point = ₹1)");
        }

        int available = getAvailablePoints(user.getId());
        if (points > available) {
            throw new InsufficientRewardPointsException(points, available);
        }

        return transferAmount.subtract(rewardValue).setScale(2, java.math.RoundingMode.HALF_UP);
    }

    @Override
    public RewardSummaryResponse getMyRewards() {
        User currentUser = userService.getCurrentUser();
        Long userId = currentUser.getId();

        int totalEarned = rewardGrantRepository.sumPointsByUserId(userId);
        int totalRedeemed = rewardRedemptionRepository.sumPointsUsedByUserId(userId);
        int available = Math.max(0, totalEarned - totalRedeemed);

        List<RewardResponse> history = rewardGrantRepository
                .findByUser_IdOrderByCreatedOnDesc(userId)
                .stream()
                .map(this::toGrantResponse)
                .toList();

        List<RewardRedemptionResponse> redemptions = rewardRedemptionRepository
                .findByUser_IdOrderByCreatedOnDesc(userId)
                .stream()
                .map(this::toRedemptionResponse)
                .toList();

        return new RewardSummaryResponse(available, totalEarned, totalRedeemed, history, redemptions);
    }

    private RewardResponse toGrantResponse(RewardGrant grant) {
        return new RewardResponse(
                grant.getId(),
                grant.getTransactionId(),
                grant.getPoints(),
                grant.getTransactionAmount(),
                grant.getCreatedOn());
    }

    private RewardRedemptionResponse toRedemptionResponse(RewardRedemption redemption) {
        return new RewardRedemptionResponse(
                redemption.getId(),
                redemption.getTransactionId(),
                redemption.getPointsUsed(),
                redemption.getRupeeValue(),
                redemption.getCreatedOn());
    }
}

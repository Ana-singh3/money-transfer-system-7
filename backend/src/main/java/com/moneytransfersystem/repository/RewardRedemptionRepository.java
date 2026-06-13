package com.moneytransfersystem.repository;

import com.moneytransfersystem.domain.entities.RewardRedemption;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RewardRedemptionRepository extends JpaRepository<RewardRedemption, String> {

    @Query("SELECT COALESCE(SUM(r.pointsUsed), 0) FROM RewardRedemption r WHERE r.user.id = :userId")
    int sumPointsUsedByUserId(@Param("userId") Long userId);

    List<RewardRedemption> findByUser_IdOrderByCreatedOnDesc(Long userId);

    Optional<RewardRedemption> findByTransactionId(String transactionId);
}

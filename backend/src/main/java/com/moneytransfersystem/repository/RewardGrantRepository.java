package com.moneytransfersystem.repository;

import com.moneytransfersystem.domain.entities.RewardGrant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RewardGrantRepository extends JpaRepository<RewardGrant, String> {

    List<RewardGrant> findByUser_IdOrderByCreatedOnDesc(Long userId);

    @Query("SELECT COALESCE(SUM(r.points), 0) FROM RewardGrant r WHERE r.user.id = :userId")
    int sumPointsByUserId(@Param("userId") Long userId);

    Optional<RewardGrant> findByTransactionId(String transactionId);
}

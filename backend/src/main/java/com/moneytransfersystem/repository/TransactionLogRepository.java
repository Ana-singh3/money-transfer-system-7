package com.moneytransfersystem.repository;

import com.moneytransfersystem.domain.entities.TransactionLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TransactionLogRepository extends JpaRepository<TransactionLog, String> {
    Optional<TransactionLog> findByIdempotencyKey(String idempotencyKey);
    List<TransactionLog> findByFromAccountIdOrToAccountIdOrderByCreatedOnDesc(String fromAccountId, String toAccountId);
}


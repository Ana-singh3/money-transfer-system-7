package com.moneytransfersystem.domain.entities;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Records reward points granted to a user for an eligible transfer.
 * Each grant is tied to exactly one successful transaction for traceability.
 */
@Entity
@Table(name = "reward_grants")
@Getter
@Setter
@NoArgsConstructor
public class RewardGrant {

    @Id
    @Column(name = "reward_id", length = 64)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    @JsonIgnore
    private User user;

    @Column(name = "transaction_id", nullable = false)
    private String transactionId;

    @Column(name = "points", nullable = false)
    private int points;

    @Column(name = "transaction_amount", precision = 19, scale = 4, nullable = false)
    private BigDecimal transactionAmount;

    @Column(name = "created_on", nullable = false)
    private Instant createdOn;

    public RewardGrant(User user, String transactionId, int points, BigDecimal transactionAmount) {
        this.user = user;
        this.transactionId = transactionId;
        this.points = points;
        this.transactionAmount = transactionAmount;
        this.createdOn = Instant.now();
    }
}

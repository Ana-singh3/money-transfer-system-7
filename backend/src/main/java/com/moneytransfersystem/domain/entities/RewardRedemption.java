package com.moneytransfersystem.domain.entities;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Records reward points spent on a transfer (1 point = ₹1).
 */
@Entity
@Table(name = "reward_redemptions")
@Getter
@Setter
@NoArgsConstructor
public class RewardRedemption {

    @Id
    @Column(name = "redemption_id", length = 64)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    @JsonIgnore
    private User user;

    @Column(name = "transaction_id", nullable = false, unique = true)
    private String transactionId;

    @Column(name = "points_used", nullable = false)
    private int pointsUsed;

    @Column(name = "rupee_value", precision = 19, scale = 4, nullable = false)
    private BigDecimal rupeeValue;

    @Column(name = "created_on", nullable = false)
    private Instant createdOn;

    public RewardRedemption(User user, String transactionId, int pointsUsed) {
        this.user = user;
        this.transactionId = transactionId;
        this.pointsUsed = pointsUsed;
        this.rupeeValue = BigDecimal.valueOf(pointsUsed).setScale(2, java.math.RoundingMode.HALF_UP);
        this.createdOn = Instant.now();
    }
}

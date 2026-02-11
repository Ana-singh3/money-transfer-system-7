package com.moneytransfersystem.domain.entities;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.moneytransfersystem.domain.enums.AccountStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "accounts")
@Getter
@Setter
@NoArgsConstructor
public class Account {
    @Id
    @Column(name = "account_id", length = 64)
    private String id;

    @Column(name = "holder_name")
    private String holderName;

    @Column(name = "balance", precision = 19, scale = 4, nullable = false)
    private BigDecimal balance;

    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    private AccountStatus status;

    @Version
    private Long version;

    @Column(name = "last_updated")
    private Instant lastUpdated;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    @JsonIgnore
    private User user;

    public Account(String id, String holderName, BigDecimal balance, AccountStatus status) {
        this.id = id;
        this.holderName = holderName;
        this.balance = balance.setScale(2, java.math.RoundingMode.HALF_UP);
        this.status = status;
        this.lastUpdated = Instant.now();
    }

    public void debit(BigDecimal amount) {
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Withdrawal amount must be positive");
        }
        BigDecimal normalizedAmount = amount.setScale(2, java.math.RoundingMode.HALF_UP);
        if (balance.compareTo(normalizedAmount) < 0) {
            throw new IllegalArgumentException("Insufficient balance");
        }
        balance = balance.subtract(normalizedAmount).setScale(2, java.math.RoundingMode.HALF_UP);
        lastUpdated = Instant.now();
    }

    public void credit(BigDecimal amount) {
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Deposit amount must be positive");
        }
        BigDecimal normalizedAmount = amount.setScale(2, java.math.RoundingMode.HALF_UP);
        balance = balance.add(normalizedAmount).setScale(2, java.math.RoundingMode.HALF_UP);
        lastUpdated = Instant.now();
    }

    public boolean isActive() { return status == AccountStatus.ACTIVE; }

    @Override
    public String toString() {
        return "Account{" +
                "id='" + id + '\'' +
                ", holderName='" + holderName + '\'' +
                ", balance=" + balance +
                ", status=" + status +
                ", version=" + version +
                ", lastUpdated=" + lastUpdated +
                '}';
    }
}

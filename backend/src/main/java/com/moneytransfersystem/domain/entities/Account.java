package com.moneytransfersystem.domain.entities;

import com.moneytransfersystem.domain.enums.AccountStatus;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "accounts")
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

    public Account() {}

    public Account(String id, String holderName, BigDecimal balance, AccountStatus status) {
        this.id = id;
        this.holderName = holderName;
        this.balance = balance;
        this.status = status;
        this.lastUpdated = Instant.now();
    }

    public void debit(BigDecimal amount) {
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Withdrawal amount must be positive");
        }
        if (balance.compareTo(amount) < 0) {
            throw new IllegalArgumentException("Insufficient balance");
        }
        balance = balance.subtract(amount);
        lastUpdated = Instant.now();
    }

    public void credit(BigDecimal amount) {
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Deposit amount must be positive");
        }
        balance = balance.add(amount);
        lastUpdated = Instant.now();
    }

    public boolean isActive() { return status == AccountStatus.ACTIVE; }


    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getHolderName() { return holderName; }
    public void setHolderName(String holderName) { this.holderName = holderName; }
    public BigDecimal getBalance() { return balance; }
    public void setBalance(BigDecimal balance) { this.balance = balance; }
    public AccountStatus getStatus() { return status; }
    public void setStatus(AccountStatus status) { this.status = status; }
    public Long getVersion() { return version; }
    public Instant getLastUpdated() { return lastUpdated; }
    public void setLastUpdated(Instant lastUpdated) { this.lastUpdated = lastUpdated; }

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

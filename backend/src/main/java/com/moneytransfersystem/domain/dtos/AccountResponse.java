// src/main/java/com/moneytransfersystem/domain/dtos/AccountResponse.java
package com.moneytransfersystem.domain.dtos;

import java.math.BigDecimal;

public class AccountResponse {
    private String accountId;
    private String holderName;
    private BigDecimal balance;
    private String status;

    public String getAccountId() { return accountId; }
    public void setAccountId(String accountId) { this.accountId = accountId; }
    public String getHolderName() { return holderName; }
    public void setHolderName(String holderName) { this.holderName = holderName; }
    public BigDecimal getBalance() { return balance; }
    public void setBalance(BigDecimal balance) { this.balance = balance; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}

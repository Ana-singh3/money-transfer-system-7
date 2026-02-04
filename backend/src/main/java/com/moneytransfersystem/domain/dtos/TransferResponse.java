// src/main/java/com/moneytransfersystem/domain/dtos/TransferResponse.java
package com.moneytransfersystem.domain.dtos;

import com.moneytransfersystem.domain.enums.TransactionStatus;
import java.math.BigDecimal;

public class TransferResponse {
    private String transactionId;
    private TransactionStatus status;
    private BigDecimal amount;
    private String message;

    public TransferResponse() {}
    public TransferResponse(String transactionId, TransactionStatus status, BigDecimal amount, String message) {
        this.transactionId = transactionId;
        this.status = status;
        this.amount = amount;
        this.message = message;
    }

    public String getTransactionId() { return transactionId; }
    public void setTransactionId(String transactionId) { this.transactionId = transactionId; }
    public TransactionStatus getStatus() { return status; }
    public void setStatus(TransactionStatus status) { this.status = status; }
    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
}

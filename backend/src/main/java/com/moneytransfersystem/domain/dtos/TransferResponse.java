package com.moneytransfersystem.domain.dtos;

import com.moneytransfersystem.domain.enums.TransactionStatus;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
public class TransferResponse {
    private String transactionId;
    private TransactionStatus status;
    private BigDecimal amount;
    private String message;
    /** Cash debited from the sender account. */
    private BigDecimal cashAmountPaid;
    /** Reward points applied (1 point = ₹1). */
    private int rewardPointsUsed;

    public TransferResponse(String transactionId, TransactionStatus status,
                            BigDecimal amount, String message) {
        this.transactionId = transactionId;
        this.status = status;
        this.amount = amount;
        this.message = message;
        this.cashAmountPaid = amount;
        this.rewardPointsUsed = 0;
    }
}

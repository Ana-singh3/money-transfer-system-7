package com.moneytransfersystem.domain.dtos;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class AccountHistoryItemDTO {
    /** TRANSFER, REWARD_CREDIT, or REWARD_DEBIT */
    private String entryType;
    private String id;
    private Instant createdOn;
    private String fromAccountId;
    private String toAccountId;
    private BigDecimal amount;
    private String status;
    private String failureReason;
    private Integer points;
    private String relatedTransactionId;
}

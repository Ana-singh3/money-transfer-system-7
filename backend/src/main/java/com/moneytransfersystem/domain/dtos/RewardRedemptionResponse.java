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
public class RewardRedemptionResponse {
    private String redemptionId;
    private String transactionId;
    private int pointsUsed;
    private BigDecimal rupeeValue;
    private Instant redeemedOn;
}

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
public class RewardResponse {
    private String rewardId;
    private String transactionId;
    private int points;
    private BigDecimal transactionAmount;
    private Instant grantedOn;
}

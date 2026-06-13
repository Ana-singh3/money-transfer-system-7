package com.moneytransfersystem.domain.dtos;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class RewardSummaryResponse {
    /** Spendable balance (earned minus redeemed). 1 point = ₹1. */
    private int availablePoints;
    private int totalEarned;
    private int totalRedeemed;
    private List<RewardResponse> history;
    private List<RewardRedemptionResponse> redemptions;
}

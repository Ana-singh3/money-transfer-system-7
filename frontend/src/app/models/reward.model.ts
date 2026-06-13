export interface RewardResponse {
  rewardId: string;
  transactionId: string;
  points: number;
  transactionAmount: number;
  grantedOn: string;
}

export interface RewardRedemptionResponse {
  redemptionId: string;
  transactionId: string;
  pointsUsed: number;
  rupeeValue: number;
  redeemedOn: string;
}

export interface RewardSummaryResponse {
  availablePoints: number;
  totalEarned: number;
  totalRedeemed: number;
  history: RewardResponse[];
  redemptions: RewardRedemptionResponse[];
}

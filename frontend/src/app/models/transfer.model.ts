export interface TransferRequest {
  fromAccountId: string;
  toAccountId: string;
  amount: number;
  idempotencyKey: string;
  rewardPointsToUse?: number;
}

export interface TransferResponse {
  transactionId: string;
  status: string;
  message: string;
  amount: number;
  cashAmountPaid?: number;
  rewardPointsUsed?: number;
}
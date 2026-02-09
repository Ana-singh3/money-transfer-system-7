export interface TransferRequest {
  fromAccountId: string;
  toAccountId: string;
  amount: number;
  idempotencyKey: string;
}

export interface TransferResponse {
  transactionId: string;
  status: string;
  message: string;
  amount: number;
}
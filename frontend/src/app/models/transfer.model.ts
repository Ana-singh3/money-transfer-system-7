export interface TransferRequest {
  fromAccountId: number;
  toAccountId: number;
  amount: number;
  idempotencyKey: string;
}

export interface TransferResponse {
  transactionId: string;
  status: string;
  message: string;
  debitedFrom: number;
  creditedTo: number;
  amount: number;
}
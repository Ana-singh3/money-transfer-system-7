export interface Transaction {
  id: string;
  fromAccountId: number;
  toAccountId: number;
  amount: number;
  status: TransactionStatus;
  failureReason?: string;
  idempotencyKey: string;
  createdOn: Date;
}

export enum TransactionStatus {
  SUCCESS = 'SUCCESS',
  FAILED = 'FAILED'
}

export interface TransactionResponse {
  id: string;
  fromAccountId: number;
  toAccountId: number;
  amount: number;
  status: string;
  createdOn: string;
  failureReason?: string;
}
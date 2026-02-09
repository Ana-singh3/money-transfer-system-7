export interface Transaction {
  id: string;
  fromAccountId: string;
  toAccountId: string;
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
  fromAccountId: string;
  toAccountId: string;
  amount: number;
  status: string;
  createdOn: string;
  failureReason?: string;
}
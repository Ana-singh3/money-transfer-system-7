export interface Account {
  id: number;
  holderName: string;
  balance: number;
  status: AccountStatus;
  version: number;
  lastUpdated: Date;
}

export enum AccountStatus {
  ACTIVE = 'ACTIVE',
  LOCKED = 'LOCKED',
  CLOSED = 'CLOSED'
}

export interface AccountResponse {
  id: number;
  holderName: string;
  balance: number;
  status: string;
}

export interface BalanceResponse {
  accountId: number;
  balance: number;
  holderName: string;
}
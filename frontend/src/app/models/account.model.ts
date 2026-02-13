export interface Account {
  accountId: string;
  holderName: string;
  balance: number;
  status: AccountStatus;
}

export enum AccountStatus {
  ACTIVE = 'ACTIVE',
  LOCKED = 'LOCKED',
  CLOSED = 'CLOSED'
}

export interface AccountResponse {
  accountId: string;
  holderName: string;
  balance: number;
  status: string;
}

export interface BalanceResponse {
  accountId: string;
  balance: number;
}
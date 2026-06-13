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
  availableRewardPoints?: number;
}

export interface AccountHistoryItem {
  entryType: 'TRANSFER' | 'REWARD_CREDIT' | 'REWARD_DEBIT';
  id: string;
  createdOn: string;
  fromAccountId?: string;
  toAccountId?: string;
  amount?: number;
  status?: string;
  failureReason?: string;
  points?: number;
  relatedTransactionId?: string;
}

export interface BalanceResponse {
  accountId: string;
  balance: number;
}
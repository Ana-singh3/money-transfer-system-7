import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, of } from 'rxjs';
import { delay } from 'rxjs/operators';
import { environment } from '../../environments/environment';
import { AccountResponse, BalanceResponse } from '../models/account.model';
import { TransactionResponse } from '../models/transaction.model';

@Injectable({
  providedIn: 'root'
})
export class AccountService {
  private apiUrl = environment.apiUrl;
  private mockMode = true; // Set to true for demo mode

  constructor(private http: HttpClient) { }

  getAccount(id: number): Observable<AccountResponse> {
    if (this.mockMode) {
      return this.mockGetAccount(id);
    }
    return this.http.get<AccountResponse>(`${this.apiUrl}/accounts/${id}`);
  }

  getBalance(id: number): Observable<BalanceResponse> {
    if (this.mockMode) {
      return this.mockGetBalance(id);
    }
    return this.http.get<BalanceResponse>(`${this.apiUrl}/accounts/${id}/balance`);
  }

  getTransactions(id: number): Observable<TransactionResponse[]> {
    if (this.mockMode) {
      return this.mockGetTransactions(id);
    }
    return this.http.get<TransactionResponse[]>(`${this.apiUrl}/accounts/${id}/transactions`);
  }

  // Mock methods
  private mockGetAccount(id: number): Observable<AccountResponse> {
    const mockAccount: AccountResponse = {
      id: id,
      holderName: 'Demo User',
      balance: 15000.00,
      status: 'ACTIVE'
    };
    return of(mockAccount).pipe(delay(300));
  }

  private mockGetBalance(id: number): Observable<BalanceResponse> {
    const mockBalance: BalanceResponse = {
      accountId: id,
      balance: 15000.00,
      holderName: 'Demo User'
    };
    return of(mockBalance).pipe(delay(300));
  }

  private mockGetTransactions(id: number): Observable<TransactionResponse[]> {
    const mockTransactions: TransactionResponse[] = [
      {
        id: 'TRX-001',
        fromAccountId: 1001,
        toAccountId: 2002,
        amount: 500.00,
        status: 'SUCCESS',
        createdOn: new Date(Date.now() - 86400000).toISOString(), // 1 day ago
      },
      {
        id: 'TRX-002',
        fromAccountId: 2002,
        toAccountId: 1001,
        amount: 1200.00,
        status: 'SUCCESS',
        createdOn: new Date(Date.now() - 172800000).toISOString(), // 2 days ago
      },
      {
        id: 'TRX-003',
        fromAccountId: 1001,
        toAccountId: 3003,
        amount: 750.50,
        status: 'SUCCESS',
        createdOn: new Date(Date.now() - 259200000).toISOString(), // 3 days ago
      },
      {
        id: 'TRX-004',
        fromAccountId: 4004,
        toAccountId: 1001,
        amount: 2000.00,
        status: 'SUCCESS',
        createdOn: new Date(Date.now() - 345600000).toISOString(), // 4 days ago
      },
      {
        id: 'TRX-005',
        fromAccountId: 1001,
        toAccountId: 5005,
        amount: 300.00,
        status: 'FAILED',
        createdOn: new Date(Date.now() - 432000000).toISOString(), // 5 days ago
        failureReason: 'Insufficient balance'
      }
    ];
    return of(mockTransactions).pipe(delay(500));
  }
}
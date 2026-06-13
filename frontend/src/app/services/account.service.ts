import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { map } from 'rxjs/operators';
import { environment } from '../../environments/environment';
import { AccountResponse, AccountHistoryItem, BalanceResponse } from '../models/account.model';
import { TransactionResponse } from '../models/transaction.model';

@Injectable({
  providedIn: 'root'
})
export class AccountService {
  private apiUrl = environment.apiUrl;

  constructor(private http: HttpClient) { }

  getAccount(id: string): Observable<AccountResponse> {
    return this.http.get<any>(`${this.apiUrl}/accounts/${id}`).pipe(
      map(a => ({
        accountId: a.accountId,
        holderName: a.holderName,
        balance: typeof a.balance === 'string' ? parseFloat(a.balance) : a.balance,
        status: a.status,
        availableRewardPoints: a.availableRewardPoints ?? 0
      }))
    );
  }

  getBalance(id: string): Observable<BalanceResponse> {
    return this.http.get<{ accountId: string; balance: number }>(`${this.apiUrl}/accounts/${id}/balance`)
      .pipe(
        map(response => ({
          accountId: response.accountId,
          balance: response.balance
        }))
      );
  }
  //GET /accounts/all
  getAllAccounts(): Observable<AccountResponse[]> {
    return this.http.get<AccountResponse[]>(`${this.apiUrl}/accounts/all`);
  }

  getTransactions(id: string): Observable<TransactionResponse[]> {
    return this.http.get<any[]>(`${this.apiUrl}/accounts/${id}/transactions`)
      .pipe(
        map(transactions => transactions.map(t => ({
          id: t.id,
          fromAccountId: t.fromAccountId,
          toAccountId: t.toAccountId,
          amount: typeof t.amount === 'string' ? parseFloat(t.amount) : t.amount,
          status: t.status,
          createdOn: t.createdOn,
          failureReason: t.failureReason
        })))
      );
  }

  getAccountHistory(id: string): Observable<AccountHistoryItem[]> {
    return this.http.get<any[]>(`${this.apiUrl}/accounts/${id}/history`).pipe(
      map(items => items.map(item => ({
        entryType: item.entryType,
        id: item.id,
        createdOn: item.createdOn,
        fromAccountId: item.fromAccountId,
        toAccountId: item.toAccountId,
        amount: item.amount != null
          ? (typeof item.amount === 'string' ? parseFloat(item.amount) : item.amount) : undefined,
        status: item.status,
        failureReason: item.failureReason,
        points: item.points,
        relatedTransactionId: item.relatedTransactionId
      })))
    );
  }

  updateAccountStatus(accountId: string, status: 'ACTIVE' | 'LOCKED'): Observable<AccountResponse> {
    return this.http.patch<any>(`${this.apiUrl}/accounts/${accountId}/status`, { status }).pipe(
      map(a => ({
        accountId: a.accountId,
        holderName: a.holderName,
        balance: typeof a.balance === 'string' ? parseFloat(a.balance) : a.balance,
        status: a.status,
        availableRewardPoints: a.availableRewardPoints ?? 0
      }))
    );
  }
}

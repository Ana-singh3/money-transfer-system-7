import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { map } from 'rxjs/operators';
import { environment } from '../../environments/environment';
import { AccountResponse, BalanceResponse } from '../models/account.model';
import { TransactionResponse } from '../models/transaction.model';

@Injectable({
  providedIn: 'root'
})
export class AccountService {
  private apiUrl = environment.apiUrl;

  constructor(private http: HttpClient) { }

  getAccount(id: string): Observable<AccountResponse> {
    return this.http.get<AccountResponse>(`${this.apiUrl}/accounts/${id}`);
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
}

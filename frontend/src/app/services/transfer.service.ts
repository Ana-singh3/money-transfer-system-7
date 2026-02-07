import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, of } from 'rxjs';
import { delay } from 'rxjs/operators';
import { environment } from '../../environments/environment';
import { TransferRequest, TransferResponse } from '../models/transfer.model';

@Injectable({
  providedIn: 'root'
})
export class TransferService {
  private apiUrl = environment.apiUrl;
  private mockMode = true; // Set to true for demo mode

  constructor(private http: HttpClient) { }

  transfer(request: TransferRequest): Observable<TransferResponse> {
    if (this.mockMode) {
      return this.mockTransfer(request);
    }
    return this.http.post<TransferResponse>(`${this.apiUrl}/transfers`, request);
  }

  generateIdempotencyKey(): string {
    return 'xxxxxxxx-xxxx-4xxx-yxxx-xxxxxxxxxxxx'.replace(/[xy]/g, function(c) {
      const r = Math.random() * 16 | 0;
      const v = c === 'x' ? r : (r & 0x3 | 0x8);
      return v.toString(16);
    });
  }

  // Mock transfer
  private mockTransfer(request: TransferRequest): Observable<TransferResponse> {
    const mockResponse: TransferResponse = {
      transactionId: `TRX-${Date.now()}`,
      status: 'SUCCESS',
      message: 'Transfer completed successfully',
      debitedFrom: request.fromAccountId,
      creditedTo: request.toAccountId,
      amount: request.amount
    };
    return of(mockResponse).pipe(delay(1000)); // Simulate network delay
  }
}
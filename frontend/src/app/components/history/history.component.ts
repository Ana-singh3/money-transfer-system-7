import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatCardModule } from '@angular/material/card';
import { MatTableModule } from '@angular/material/table';
import { MatChipsModule } from '@angular/material/chips';
import { MatIconModule } from '@angular/material/icon';
import { MatButtonModule } from '@angular/material/button';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { NavbarComponent } from '../navbar/navbar.component';
import { AuthService } from '../../services/auth.service';
import { AccountService } from '../../services/account.service';
import { TransactionResponse } from '../../models/transaction.model';

@Component({
  selector: 'app-history',
  standalone: true,
  imports: [
    CommonModule,
    MatCardModule,
    MatTableModule,
    MatChipsModule,
    MatIconModule,
    MatButtonModule,
    MatProgressSpinnerModule,
    MatSnackBarModule,
    NavbarComponent
  ],
  templateUrl: './history.component.html',
  styleUrls: ['./history.component.scss']
})
export class HistoryComponent implements OnInit {
  accountId: number = 0;
  transactions: TransactionResponse[] = [];
  displayedColumns: string[] = ['date', 'type', 'account', 'amount', 'status'];
  loading = true;
  noTransactions = false;

  constructor(
    private authService: AuthService,
    private accountService: AccountService,
    private snackBar: MatSnackBar
  ) {}

  ngOnInit(): void {
    const currentUser = this.authService.currentUserValue;
    if (currentUser) {
      this.accountId = currentUser.accountId;
      this.loadTransactions();
    }
  }

  loadTransactions(): void {
    this.loading = true;
    this.accountService.getTransactions(this.accountId).subscribe({
      next: (transactions: TransactionResponse[]) => {
        this.transactions = transactions.sort((a, b) => {
          return new Date(b.createdOn).getTime() - new Date(a.createdOn).getTime();
        });
        this.loading = false;
        this.noTransactions = transactions.length === 0;
      },
      error: (error) => {
        this.loading = false;
        this.snackBar.open('Failed to load transactions', 'Close', {
          duration: 3000,
          panelClass: ['error-snackbar']
        });
      }
    });
  }

  getTransactionType(transaction: TransactionResponse): string {
    return transaction.fromAccountId === this.accountId ? 'DEBIT' : 'CREDIT';
  }

  getOtherAccount(transaction: TransactionResponse): number {
    return transaction.fromAccountId === this.accountId 
      ? transaction.toAccountId 
      : transaction.fromAccountId;
  }

  formatDate(dateString: string): string {
    const date = new Date(dateString);
    return date.toLocaleDateString('en-IN', { 
      year: 'numeric', 
      month: 'short', 
      day: 'numeric',
      hour: '2-digit',
      minute: '2-digit'
    });
  }

  refreshTransactions(): void {
    this.loadTransactions();
    this.snackBar.open('Transactions refreshed', 'Close', {
      duration: 2000,
      panelClass: ['success-snackbar']
    });
  }
}
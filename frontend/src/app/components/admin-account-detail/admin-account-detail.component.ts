import { Component, OnInit, AfterViewInit, ElementRef, ViewChild } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, Router } from '@angular/router';
import { MatCardModule } from '@angular/material/card';
import { MatTableModule } from '@angular/material/table';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { MatChipsModule } from '@angular/material/chips';
import { NavbarComponent } from '../navbar/navbar.component';
import { AuthService } from '../../services/auth.service';
import { AccountService } from '../../services/account.service';
import { AccountResponse } from '../../models/account.model';
import { TransactionResponse } from '../../models/transaction.model';
import { AnimationsService } from '../../utils/animations.service';

@Component({
  selector: 'app-admin-account-detail',
  standalone: true,
  imports: [
    CommonModule,
    MatCardModule,
    MatTableModule,
    MatButtonModule,
    MatIconModule,
    MatProgressSpinnerModule,
    MatSnackBarModule,
    MatChipsModule,
    NavbarComponent
  ],
  templateUrl: './admin-account-detail.component.html',
  styleUrls: ['./admin-account-detail.component.scss']
})
export class AdminAccountDetailComponent implements OnInit, AfterViewInit {
  @ViewChild('accountCard', { static: false }) accountCard!: ElementRef;
  @ViewChild('transactionsCard', { static: false }) transactionsCard!: ElementRef;
  
  accountId: string = '';
  account: AccountResponse | null = null;
  transactions: TransactionResponse[] = [];
  displayedColumns: string[] = ['date', 'fromAccount', 'toAccount', 'amount', 'status'];
  loading = true;
  loadingTransactions = true;

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private authService: AuthService,
    private accountService: AccountService,
    private snackBar: MatSnackBar,
    private animationsService: AnimationsService
  ) {}

  ngOnInit(): void {
    const currentUser = this.authService.currentUserValue;
    if (!currentUser || currentUser.role !== 'ROLE_ADMIN') {
      this.router.navigate(['/auth']);
      return;
    }

    this.accountId = this.route.snapshot.paramMap.get('id') || '';
    if (this.accountId) {
      this.loadAccountDetails();
      this.loadTransactions();
    }
  }

  ngAfterViewInit(): void {
    if (this.accountCard) {
      this.animationsService.fadeIn(this.accountCard.nativeElement, 0.6);
    }
    if (this.transactionsCard) {
      this.animationsService.fadeIn(this.transactionsCard.nativeElement, 0.6);
    }
  }

  loadAccountDetails(): void {
    this.loading = true;
    this.accountService.getAccount(this.accountId).subscribe({
      next: (account: AccountResponse) => {
        this.account = account;
        this.loading = false;
      },
      error: (error) => {
        this.loading = false;
        this.snackBar.open('Failed to load account details', 'Close', {
          duration: 3000,
          panelClass: ['error-snackbar']
        });
        this.router.navigate(['/admin/dashboard']);
      }
    });
  }

  loadTransactions(): void {
    this.loadingTransactions = true;
    this.accountService.getTransactions(this.accountId).subscribe({
      next: (transactions: TransactionResponse[]) => {
        this.transactions = transactions.sort((a, b) => {
          return new Date(b.createdOn).getTime() - new Date(a.createdOn).getTime();
        });
        this.loadingTransactions = false;
        
        setTimeout(() => {
          const rows = document.querySelectorAll('.transactions-table tbody tr');
          if (rows.length > 0) {
            this.animationsService.staggerFadeIn(Array.from(rows) as HTMLElement[], 0.05);
          }
        }, 100);
      },
      error: (error) => {
        this.loadingTransactions = false;
        this.snackBar.open('Failed to load transactions', 'Close', {
          duration: 3000,
          panelClass: ['error-snackbar']
        });
      }
    });
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

  formatBalance(balance: number): string {
    return `₹${balance.toFixed(2)}`;
  }

  goBack(): void {
    this.router.navigate(['/admin/dashboard']);
  }
}


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
import { AccountHistoryItem, AccountResponse } from '../../models/account.model';
import { AnimationsService } from '../../utils/animations.service';

@Component({
  selector: 'app-admin-account-detail',
  standalone: true,
  imports: [
    CommonModule, MatCardModule, MatTableModule, MatButtonModule,
    MatIconModule, MatProgressSpinnerModule, MatSnackBarModule,
    MatChipsModule, NavbarComponent
  ],
  templateUrl: './admin-account-detail.component.html',
  styleUrls: ['./admin-account-detail.component.scss']
})
export class AdminAccountDetailComponent implements OnInit, AfterViewInit {
  @ViewChild('accountCard', { static: false }) accountCard!: ElementRef;
  @ViewChild('transactionsCard', { static: false }) transactionsCard!: ElementRef;

  accountId = '';
  account: AccountResponse | null = null;
  history: AccountHistoryItem[] = [];
  displayedColumns = ['date', 'type', 'fromAccount', 'toAccount', 'amount', 'points', 'status'];
  loading = true;
  loadingHistory = true;
  updatingStatus = false;

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private authService: AuthService,
    private accountService: AccountService,
    private snackBar: MatSnackBar,
    private animationsService: AnimationsService
  ) {}

  ngOnInit(): void {
    const user = this.authService.currentUserValue;
    if (!user || user.role !== 'ROLE_ADMIN') {
      this.router.navigate(['/auth']);
      return;
    }
    this.accountId = this.route.snapshot.paramMap.get('id') || '';
    if (this.accountId) {
      this.loadAccountDetails();
      this.loadHistory();
    }
  }

  ngAfterViewInit(): void {
    if (this.accountCard) this.animationsService.fadeIn(this.accountCard.nativeElement, 0.6);
    if (this.transactionsCard) this.animationsService.fadeIn(this.transactionsCard.nativeElement, 0.6);
  }

  loadAccountDetails(): void {
    this.loading = true;
    this.accountService.getAccount(this.accountId).subscribe({
      next: (account) => { this.account = account; this.loading = false; },
      error: () => {
        this.loading = false;
        this.snackBar.open('Failed to load account details', 'Close', { duration: 3000 });
        this.router.navigate(['/admin/dashboard']);
      }
    });
  }

  loadHistory(): void {
    this.loadingHistory = true;
    this.accountService.getAccountHistory(this.accountId).subscribe({
      next: (items) => {
        this.history = items;
        this.loadingHistory = false;
      },
      error: () => {
        this.loadingHistory = false;
        this.snackBar.open('Failed to load history', 'Close', { duration: 3000 });
      }
    });
  }

  toggleAccountStatus(): void {
    if (!this.account || this.updatingStatus) return;
    const newStatus = this.account.status === 'ACTIVE' ? 'LOCKED' : 'ACTIVE';
    const action = newStatus === 'LOCKED' ? 'deactivated' : 'activated';
    this.updatingStatus = true;
    this.accountService.updateAccountStatus(this.accountId, newStatus).subscribe({
      next: (updated) => {
        this.account = updated;
        this.updatingStatus = false;
        this.snackBar.open(`Account ${action} successfully`, 'Close', { duration: 3000 });
      },
      error: () => {
        this.updatingStatus = false;
        this.snackBar.open('Failed to update account status', 'Close', { duration: 3000 });
      }
    });
  }

  getEntryTypeLabel(item: AccountHistoryItem): string {
    if (item.entryType === 'REWARD_CREDIT') return 'Reward Credit';
    if (item.entryType === 'REWARD_DEBIT') return 'Reward Debit';
    return 'Transfer';
  }

  formatDate(dateString: string): string {
    return new Date(dateString).toLocaleDateString('en-IN', {
      year: 'numeric', month: 'short', day: 'numeric', hour: '2-digit', minute: '2-digit'
    });
  }

  formatBalance(balance: number): string {
    return `₹${balance.toFixed(2)}`;
  }

  formatPoints(item: AccountHistoryItem): string {
    if (item.points == null) return '—';
    return item.points > 0 ? `+${item.points}` : `${item.points}`;
  }

  goBack(): void {
    this.router.navigate(['/admin/dashboard']);
  }
}

import { Component, OnInit, AfterViewInit, ElementRef, ViewChild } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router } from '@angular/router';
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
import { AnimationsService } from '../../utils/animations.service';

@Component({
  selector: 'app-admin-dashboard',
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
  templateUrl: './admin-dashboard.component.html',
  styleUrls: ['./admin-dashboard.component.scss']
})
export class AdminDashboardComponent implements OnInit, AfterViewInit {
  @ViewChild('accountsCard', { static: false }) accountsCard!: ElementRef;
  
  accounts: AccountResponse[] = [];
  displayedColumns: string[] = ['accountId', 'holderName', 'balance', 'status', 'actions'];
  loading = true;
  adminName: string = '';

  constructor(
    private authService: AuthService,
    private accountService: AccountService,
    private router: Router,
    private snackBar: MatSnackBar,
    private animationsService: AnimationsService
  ) {}

  ngOnInit(): void {
    const currentUser = this.authService.currentUserValue;
    if (currentUser && currentUser.role === 'ROLE_ADMIN') {
      this.adminName = currentUser.username;
      this.loadAccounts();
    } else {
      this.router.navigate(['/auth']);
    }
  }

  ngAfterViewInit(): void {
    if (this.accountsCard) {
      this.animationsService.fadeIn(this.accountsCard.nativeElement, 0.6);
    }
  }

  loadAccounts(): void {
    this.loading = true;
    this.accountService.getAllAccounts().subscribe({
      next: (accounts: AccountResponse[]) => {
        this.accounts = accounts;
        this.loading = false;
        
        setTimeout(() => {
          const rows = document.querySelectorAll('.accounts-table tbody tr');
          if (rows.length > 0) {
            this.animationsService.staggerFadeIn(Array.from(rows) as HTMLElement[], 0.05);
          }
        }, 100);
      },
      error: (error) => {
        this.loading = false;
        this.snackBar.open('Failed to load accounts', 'Close', {
          duration: 3000,
          panelClass: ['error-snackbar']
        });
      }
    });
  }
  ///admin/account/:id
  viewAccountDetails(accountId: string): void {
    this.router.navigate(['/admin/account', accountId]);
  }

  formatBalance(balance: number): string {
    return `₹${balance.toFixed(2)}`;
  }

  refreshAccounts(): void {
    this.loadAccounts();
    this.snackBar.open('Accounts refreshed', 'Close', {
      duration: 2000,
      panelClass: ['success-snackbar']
    });
  }
}


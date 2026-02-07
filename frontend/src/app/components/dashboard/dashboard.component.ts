import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router } from '@angular/router';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { NavbarComponent } from '../navbar/navbar.component';
import { AuthService } from '../../services/auth.service';
import { AccountService } from '../../services/account.service';
import { BalanceResponse } from '../../models/account.model';
import { MatTooltipModule } from '@angular/material/tooltip';


@Component({
  selector: 'app-dashboard',
  standalone: true,
  imports: [
    CommonModule,
    MatCardModule,
    MatButtonModule,
    MatIconModule,
    MatProgressSpinnerModule,
    MatSnackBarModule,
    MatTooltipModule,
    NavbarComponent
  ],
  templateUrl: './dashboard.component.html',
  styleUrls: ['./dashboard.component.scss']
})
export class DashboardComponent implements OnInit {
  holderName: string = '';
  accountId: number = 0;
  balance: number = 0;
  loading: boolean = true;

  constructor(
    private authService: AuthService,
    private accountService: AccountService,
    private router: Router,
    private snackBar: MatSnackBar
  ) {}

  ngOnInit(): void {
    const currentUser = this.authService.currentUserValue;
    if (currentUser) {
      this.holderName = currentUser.holderName;
      this.accountId = currentUser.accountId;
      this.loadBalance();
    }
  }

  loadBalance(): void {
    this.loading = true;
    this.accountService.getBalance(this.accountId).subscribe({
      next: (response: BalanceResponse) => {
        this.balance = response.balance;
        this.loading = false;
      },
      error: (error) => {
        this.loading = false;
        this.snackBar.open('Failed to load balance', 'Close', {
          duration: 3000,
          panelClass: ['error-snackbar']
        });
      }
    });
  }

  navigateToTransfer(): void {
    this.router.navigate(['/transfer']);
  }

  navigateToHistory(): void {
    this.router.navigate(['/history']);
  }

  refreshBalance(): void {
    this.loadBalance();
    this.snackBar.open('Balance refreshed', 'Close', {
      duration: 2000,
      panelClass: ['success-snackbar']
    });
  }
}
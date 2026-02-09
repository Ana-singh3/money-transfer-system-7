import { Component, OnInit, AfterViewInit, ElementRef, ViewChild } from '@angular/core';
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
import { AnimationsService } from '../../utils/animations.service';

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
export class DashboardComponent implements OnInit, AfterViewInit {
  @ViewChild('balanceCard', { static: false }) balanceCard!: ElementRef;
  @ViewChild('balanceAmount', { static: false }) balanceAmount!: ElementRef;
  @ViewChild('actionCards', { static: false }) actionCards!: ElementRef;
  
  holderName: string = '';
  accountId: string = '';
  balance: number = 0;
  displayBalance: number = 0;
  loading: boolean = true;

  constructor(
    private authService: AuthService,
    private accountService: AccountService,
    private router: Router,
    private snackBar: MatSnackBar,
    private animationsService: AnimationsService
  ) {}

  ngOnInit(): void {
    const currentUser = this.authService.currentUserValue;
    if (currentUser && currentUser.accountId) {
      this.holderName = currentUser.holderName || currentUser.username;
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
        
        if (this.balanceAmount) {
          this.animationsService.countUp(
            this.balanceAmount.nativeElement,
            this.displayBalance,
            this.balance,
            1.5
          );
        }
        this.displayBalance = this.balance;
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

  ngAfterViewInit(): void {
    if (this.balanceCard) {
      this.animationsService.fadeIn(this.balanceCard.nativeElement, 0.8);
    }

    if (this.actionCards) {
      const cards = this.actionCards.nativeElement.querySelectorAll('.action-card');
      this.animationsService.staggerFadeIn(Array.from(cards), 0.15);
    }
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
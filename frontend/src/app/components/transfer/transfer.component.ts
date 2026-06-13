import { Component, OnInit, AfterViewInit, ElementRef, ViewChild, OnDestroy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, Validators, ReactiveFormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { MatCardModule } from '@angular/material/card';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { MatTooltipModule } from '@angular/material/tooltip';
import { MatCheckboxModule } from '@angular/material/checkbox';
import { NavbarComponent } from '../navbar/navbar.component';
import { AuthService } from '../../services/auth.service';
import { TransferService } from '../../services/transfer.service';
import { AccountService } from '../../services/account.service';
import { RewardService } from '../../services/reward.service';
import { TransferRequest, TransferResponse } from '../../models/transfer.model';
import { BalanceResponse } from '../../models/account.model';
import { AnimationsService } from '../../utils/animations.service';

@Component({
  selector: 'app-transfer',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    MatCardModule,
    MatFormFieldModule,
    MatInputModule,
    MatButtonModule,
    MatIconModule,
    MatProgressSpinnerModule,
    MatSnackBarModule,
    MatTooltipModule,
    MatCheckboxModule,
    NavbarComponent
  ],
  templateUrl: './transfer.component.html',
  styleUrls: ['./transfer.component.scss']
})
export class TransferComponent implements OnInit, AfterViewInit, OnDestroy {
  @ViewChild('successMessage', { static: false }) successMessage!: ElementRef;
  @ViewChild('transferFormCard', { static: false }) transferFormCard!: ElementRef;
  
  transferForm!: FormGroup;
  loading = false;
  accountId: string = '';
  holderName: string = '';
  currentBalance: number = 0;
  availableRewardPoints: number = 0;
  loadingBalance = true;
  loadingRewards = true;
  transferSuccess = false;
  transferResult: TransferResponse | null = null;
  redirecting = false;
  redirectCountdown = 10;
  private redirectTimer: any = null;

  constructor(
    private formBuilder: FormBuilder,
    private authService: AuthService,
    private transferService: TransferService,
    private accountService: AccountService,
    private rewardService: RewardService,
    private router: Router,
    private snackBar: MatSnackBar,
    private animationsService: AnimationsService
  ) {}

  ngOnInit(): void {
    const currentUser = this.authService.currentUserValue;
    if (currentUser && currentUser.accountId) {
      this.accountId = currentUser.accountId;
      this.holderName = currentUser.holderName || currentUser.username;
      this.loadBalance();
      this.loadRewardPoints();
    }

    this.transferForm = this.formBuilder.group({
      toAccountId: ['', [Validators.required, Validators.pattern('^ACC-[A-Z0-9]{16}$')]],
      amount: ['', [Validators.required, Validators.min(0.01), Validators.pattern('^[0-9]+(\\.[0-9]{1,2})?$')]],
      useRewardPoints: [false],
      rewardPointsToUse: [{ value: 0, disabled: true }, [Validators.min(0)]]
    });

    this.transferForm.get('useRewardPoints')?.valueChanges.subscribe((use: boolean) => {
      const pointsControl = this.transferForm.get('rewardPointsToUse');
      if (use) {
        pointsControl?.enable();
        this.syncRewardPointsToUse();
      } else {
        pointsControl?.disable();
        pointsControl?.setValue(0);
      }
    });

    this.transferForm.get('amount')?.valueChanges.subscribe(() => {
      if (this.transferForm.get('useRewardPoints')?.value) {
        this.syncRewardPointsToUse();
      }
    });
  }

  loadRewardPoints(): void {
    this.loadingRewards = true;
    this.rewardService.getMyRewards().subscribe({
      next: (summary) => {
        this.availableRewardPoints = summary.availablePoints;
        this.loadingRewards = false;
      },
      error: () => {
        this.loadingRewards = false;
      }
    });
  }

  /** Auto-fill max usable points: min(transfer amount floor, available balance). */
  syncRewardPointsToUse(): void {
    const amount = parseFloat(this.transferForm.get('amount')?.value) || 0;
    const maxPoints = Math.min(Math.floor(amount), this.availableRewardPoints);
    this.transferForm.get('rewardPointsToUse')?.setValue(maxPoints);
  }

  get cashPortion(): number {
    const amount = parseFloat(this.transferForm.get('amount')?.value) || 0;
    const points = this.transferForm.get('useRewardPoints')?.value
      ? (parseInt(this.transferForm.get('rewardPointsToUse')?.value, 10) || 0) : 0;
    return Math.max(0, Math.round((amount - points) * 100) / 100);
  }

  get rewardPortion(): number {
    if (!this.transferForm.get('useRewardPoints')?.value) return 0;
    return parseInt(this.transferForm.get('rewardPointsToUse')?.value, 10) || 0;
  }

  ngAfterViewInit(): void {
    if (this.transferFormCard) {
      this.animationsService.fadeIn(this.transferFormCard.nativeElement, 0.6);
    }
  }

  loadBalance(): void {
    this.loadingBalance = true;
    this.accountService.getBalance(this.accountId).subscribe({
      next: (response: BalanceResponse) => {
        this.currentBalance = response.balance;
        this.loadingBalance = false;
      },
      error: (error) => {
        this.loadingBalance = false;
        this.snackBar.open('Failed to load balance', 'Close', {
          duration: 3000,
          panelClass: ['error-snackbar']
        });
      }
    });
  }

  get f() {
    return this.transferForm.controls;
  }

  onSubmit(): void {
    if (this.transferForm.invalid) {
      Object.keys(this.transferForm.controls).forEach(key => {
        this.transferForm.controls[key].markAsTouched();
      });
      return;
    }

    const toAccountId = this.transferForm.value.toAccountId.trim();
    const amountValue = parseFloat(this.transferForm.value.amount);
    const amount = Math.round(amountValue * 100) / 100;

    if (toAccountId === this.accountId) {
      this.snackBar.open('Cannot transfer to the same account', 'Close', {
        duration: 4000,
        panelClass: ['error-snackbar']
      });
      return;
    }

    const useRewards = this.transferForm.get('useRewardPoints')?.value;
    const rewardPoints = useRewards ? (parseInt(this.transferForm.get('rewardPointsToUse')?.value, 10) || 0) : 0;

    if (useRewards && rewardPoints > 0) {
      if (rewardPoints > this.availableRewardPoints) {
        this.snackBar.open('Insufficient reward points', 'Close', {
          duration: 4000,
          panelClass: ['error-snackbar']
        });
        return;
      }
      if (rewardPoints > amount) {
        this.snackBar.open('Reward points cannot exceed transfer amount', 'Close', {
          duration: 4000,
          panelClass: ['error-snackbar']
        });
        return;
      }
    }

    if (this.cashPortion > this.currentBalance) {
      this.snackBar.open(
        useRewards ? 'Insufficient cash balance for remaining amount' : 'Insufficient balance',
        'Close', { duration: 4000, panelClass: ['error-snackbar'] }
      );
      return;
    }

    this.executeTransfer(toAccountId, amount, rewardPoints);
  }

  executeTransfer(toAccountId: string, amount: number, rewardPointsToUse: number = 0): void {
    this.loading = true;
    this.transferSuccess = false;

    const transferRequest: TransferRequest = {
      fromAccountId: this.accountId,
      toAccountId: toAccountId,
      amount: amount,
      idempotencyKey: this.transferService.generateIdempotencyKey(),
      rewardPointsToUse: rewardPointsToUse > 0 ? rewardPointsToUse : undefined
    };

    this.transferService.transfer(transferRequest).subscribe({
      next: (response: TransferResponse) => {
        this.loading = false;
        this.transferSuccess = true;
        this.transferResult = response;
        
        setTimeout(() => {
          if (this.successMessage) {
            this.animationsService.scaleIn(this.successMessage.nativeElement, 0.6);
          }
        }, 100);
        
        this.snackBar.open('Transfer successful! ', 'Close', {
          duration: 5000,
          panelClass: ['success-snackbar']
        });

        this.loadBalance();
        this.loadRewardPoints();

        // start redirect countdown and show message
        this.startRedirectCountdown(10);
      },
      error: (error) => {
        this.loading = false;
        this.transferSuccess = false;
        
        const errorMessage = error.error?.message || 'Transfer failed. Please try again.';
        this.snackBar.open(errorMessage, 'Close', {
          duration: 5000,
          panelClass: ['error-snackbar']
        });
      }
    });
  }

  resetForm(): void {
    this.transferForm.reset();
    this.transferSuccess = false;
    this.transferResult = null;
  }

  startRedirectCountdown(seconds: number) {
    if (this.redirectTimer) {
      clearInterval(this.redirectTimer);
    }
    this.redirecting = true;
    this.redirectCountdown = seconds;
    this.redirectTimer = setInterval(() => {
      this.redirectCountdown -= 1;
      if (this.redirectCountdown <= 0) {
        clearInterval(this.redirectTimer);
        this.redirectTimer = null;
        this.redirecting = false;
        // navigate back to transfer page (refresh) and reset form
        this.resetForm();
        this.router.navigate(['/transfer']);
      }
    }, 1000);
  }

  ngOnDestroy(): void {
    if (this.redirectTimer) {
      clearInterval(this.redirectTimer);
      this.redirectTimer = null;
    }
  }

  cancel(): void {
    this.router.navigate(['/dashboard']);
  }
}
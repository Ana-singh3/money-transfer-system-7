import { Component, OnInit, AfterViewInit, ElementRef, ViewChild } from '@angular/core';
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
import { NavbarComponent } from '../navbar/navbar.component';
import { AuthService } from '../../services/auth.service';
import { TransferService } from '../../services/transfer.service';
import { AccountService } from '../../services/account.service';
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
    NavbarComponent
  ],
  templateUrl: './transfer.component.html',
  styleUrls: ['./transfer.component.scss']
})
export class TransferComponent implements OnInit, AfterViewInit {
  @ViewChild('successMessage', { static: false }) successMessage!: ElementRef;
  @ViewChild('transferFormCard', { static: false }) transferFormCard!: ElementRef;
  
  transferForm!: FormGroup;
  loading = false;
  accountId: string = '';
  holderName: string = '';
  currentBalance: number = 0;
  loadingBalance = true;
  transferSuccess = false;
  transferResult: TransferResponse | null = null;

  constructor(
    private formBuilder: FormBuilder,
    private authService: AuthService,
    private transferService: TransferService,
    private accountService: AccountService,
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
    }

    this.transferForm = this.formBuilder.group({
      toAccountId: ['', [Validators.required, Validators.pattern('^[A-Z0-9-]+$')]],
      amount: ['', [Validators.required, Validators.min(0.01), Validators.pattern('^[0-9]+(\\.[0-9]{1,2})?$')]]
    });
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

    if (amount > this.currentBalance) {
      this.snackBar.open('Insufficient balance', 'Close', {
        duration: 4000,
        panelClass: ['error-snackbar']
      });
      return;
    }

    this.executeTransfer(toAccountId, amount);
  }

  executeTransfer(toAccountId: string, amount: number): void {
    this.loading = true;
    this.transferSuccess = false;

    const transferRequest: TransferRequest = {
      fromAccountId: this.accountId,
      toAccountId: toAccountId,
      amount: amount,
      idempotencyKey: this.transferService.generateIdempotencyKey()
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
        
        this.snackBar.open('Transfer successful! 🎉', 'Close', {
          duration: 5000,
          panelClass: ['success-snackbar']
        });

        this.loadBalance();

        setTimeout(() => {
          this.resetForm();
        }, 10000);
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

  cancel(): void {
    this.router.navigate(['/dashboard']);
  }
}
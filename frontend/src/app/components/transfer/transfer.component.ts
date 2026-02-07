import { Component, OnInit } from '@angular/core';
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
import { MatDialog, MatDialogModule } from '@angular/material/dialog';
import { MatTooltipModule } from '@angular/material/tooltip';
import { NavbarComponent } from '../navbar/navbar.component';
import { AuthService } from '../../services/auth.service';
import { TransferService } from '../../services/transfer.service';
import { AccountService } from '../../services/account.service';
import { TransferRequest, TransferResponse } from '../../models/transfer.model';
import { BalanceResponse } from '../../models/account.model';

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
    MatDialogModule,
    MatTooltipModule,
    NavbarComponent
  ],
  templateUrl: './transfer.component.html',
  styleUrls: ['./transfer.component.scss']
})
export class TransferComponent implements OnInit {
  transferForm!: FormGroup;
  loading = false;
  accountId: number = 0;
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
    private snackBar: MatSnackBar
  ) {}

  ngOnInit(): void {
    const currentUser = this.authService.currentUserValue;
    if (currentUser) {
      this.accountId = currentUser.accountId;
      this.holderName = currentUser.holderName;
      this.loadBalance();
    }

    this.transferForm = this.formBuilder.group({
      toAccountId: ['', [Validators.required, Validators.pattern('^[0-9]+$')]],
      amount: ['', [Validators.required, Validators.min(1), Validators.pattern('^[0-9]+(\\.[0-9]{1,2})?$')]]
    });
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

    const toAccountId = parseInt(this.transferForm.value.toAccountId);
    const amount = parseFloat(this.transferForm.value.amount);

    // Validation: Check if transferring to same account
    if (toAccountId === this.accountId) {
      this.snackBar.open('Cannot transfer to the same account', 'Close', {
        duration: 4000,
        panelClass: ['error-snackbar']
      });
      return;
    }

    // Validation: Check if sufficient balance
    if (amount > this.currentBalance) {
      this.snackBar.open('Insufficient balance', 'Close', {
        duration: 4000,
        panelClass: ['error-snackbar']
      });
      return;
    }

    this.executeTransfer(toAccountId, amount);
  }

  executeTransfer(toAccountId: number, amount: number): void {
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
        
        this.snackBar.open('Transfer successful! 🎉', 'Close', {
          duration: 5000,
          panelClass: ['success-snackbar']
        });

        // Reload balance
        this.loadBalance();

        // Reset form after 2 seconds
        setTimeout(() => {
          this.resetForm();
        }, 3000);
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
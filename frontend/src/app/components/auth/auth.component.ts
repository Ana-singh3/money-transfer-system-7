import { Component, OnInit, AfterViewInit, ElementRef, ViewChild } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, Validators, ReactiveFormsModule } from '@angular/forms';
import { Router, ActivatedRoute } from '@angular/router';
import { MatCardModule } from '@angular/material/card';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { MatTabsModule } from '@angular/material/tabs';
import { AuthService } from '../../services/auth.service';
import { AnimationsService } from '../../utils/animations.service';

type AuthMode = 'signup' | 'login' | 'admin';

@Component({
  selector: 'app-auth',
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
    MatTabsModule
  ],
  templateUrl: './auth.component.html',
  styleUrls: ['./auth.component.scss']
})
export class AuthComponent implements OnInit, AfterViewInit {
  @ViewChild('authCard', { static: false }) authCard!: ElementRef;
  
  authMode: AuthMode = 'login';
  signupForm!: FormGroup;
  loginForm!: FormGroup;
  adminForm!: FormGroup;
  
  loading = false;
  hidePassword = true;
  hideSignupPassword = true;
  hideAdminPassword = true;
  returnUrl: string = '';

  constructor(
    private formBuilder: FormBuilder,
    private authService: AuthService,
    private router: Router,
    private route: ActivatedRoute,
    private snackBar: MatSnackBar,
    private animationsService: AnimationsService
  ) {}

  ngOnInit(): void {
    if (this.authService.isAuthenticated()) {
      const user = this.authService.currentUserValue;
      if (user?.role === 'ROLE_ADMIN') {
        this.router.navigate(['/admin/dashboard']);
      } else {
        this.router.navigate(['/dashboard']);
      }
      return;
    }

    this.signupForm = this.formBuilder.group({
      username: ['', [Validators.required, Validators.minLength(3)]],
      password: ['', [Validators.required, Validators.minLength(4)]],
      confirmPassword: ['', [Validators.required]]
    }, { validators: this.passwordMatchValidator });

    this.loginForm = this.formBuilder.group({
      username: ['', [Validators.required]],
      password: ['', [Validators.required, Validators.minLength(4)]]
    });

    this.adminForm = this.formBuilder.group({
      username: ['', [Validators.required]],
      password: ['', [Validators.required, Validators.minLength(4)]]
    });

    this.returnUrl = this.route.snapshot.queryParams['returnUrl'] || '/dashboard';
  }

  ngAfterViewInit(): void {
    if (this.authCard) {
      this.animationsService.scaleIn(this.authCard.nativeElement, 0.8);
    }
  }

  passwordMatchValidator(form: FormGroup) {
    const password = form.get('password');
    const confirmPassword = form.get('confirmPassword');
    if (password && confirmPassword && password.value !== confirmPassword.value) {
      confirmPassword.setErrors({ passwordMismatch: true });
      return { passwordMismatch: true };
    }
    return null;
  }

  switchMode(mode: AuthMode): void {
    this.authMode = mode;
  }

  onSignup(): void {
    if (this.signupForm.invalid) {
      return;
    }

    this.loading = true;
    const { username, password } = this.signupForm.value;

    this.authService.signup(username, password).subscribe({
      next: (response) => {
        this.loading = false;
        this.snackBar.open('Account created successfully! Please login.', 'Close', {
          duration: 3000,
          panelClass: ['success-snackbar']
        });
        this.switchMode('login');
        this.loginForm.patchValue({ username });
      },
      error: (error) => {
        this.loading = false;
        const errorMessage = error.error?.message || 'Signup failed. Please try again.';
        this.snackBar.open(errorMessage, 'Close', {
          duration: 5000,
          panelClass: ['error-snackbar']
        });
      }
    });
  }

  onLogin(): void {
    if (this.loginForm.invalid) {
      return;
    }

    this.loading = true;
    const { username, password } = this.loginForm.value;

    this.authService.login(username, password).subscribe({
      next: (response) => {
        this.loading = false;
        this.snackBar.open('Login successful!', 'Close', {
          duration: 3000,
          panelClass: ['success-snackbar']
        });
        this.router.navigate([this.returnUrl]);
      },
      error: (error) => {
        this.loading = false;
        let errorMessage = 'Invalid username or password';
        
        if (error.status === 0) {
          errorMessage = 'Cannot connect to server. Please check if backend is running.';
        } else if (error.error?.message) {
          errorMessage = error.error.message;
        }
        
        this.snackBar.open(errorMessage, 'Close', {
          duration: 5000,
          panelClass: ['error-snackbar']
        });
      }
    });
  }

  onAdminLogin(): void {
    if (this.adminForm.invalid) {
      return;
    }

    this.loading = true;
    const { username, password } = this.adminForm.value;

    this.authService.login(username, password).subscribe({
      next: (response) => {
        this.loading = false;
        if (response.role === 'ROLE_ADMIN') {
          this.snackBar.open('Admin login successful!', 'Close', {
            duration: 3000,
            panelClass: ['success-snackbar']
          });
          this.router.navigate(['/admin/dashboard']);
        } else {
          this.snackBar.open('This account is not an admin account', 'Close', {
            duration: 5000,
            panelClass: ['error-snackbar']
          });
        }
      },
      error: (error) => {
        this.loading = false;
        const errorMessage = error.error?.message || 'Invalid admin credentials';
        this.snackBar.open(errorMessage, 'Close', {
          duration: 5000,
          panelClass: ['error-snackbar']
        });
      }
    });
  }

  get signupF() { return this.signupForm.controls; }
  get loginF() { return this.loginForm.controls; }
  get adminF() { return this.adminForm.controls; }
}


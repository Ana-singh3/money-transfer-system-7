import { Component, OnInit, AfterViewInit, ElementRef, ViewChild } from '@angular/core';
import { Router } from '@angular/router';
import { CommonModule } from '@angular/common';
import { MatCardModule } from '@angular/material/card';
import { MatTableModule } from '@angular/material/table';
import { MatIconModule } from '@angular/material/icon';
import { MatButtonModule } from '@angular/material/button';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { NavbarComponent } from '../navbar/navbar.component';
import { RewardService } from '../../services/reward.service';
import { RewardResponse, RewardRedemptionResponse } from '../../models/reward.model';
import { AnimationsService } from '../../utils/animations.service';

@Component({
  selector: 'app-rewards',
  standalone: true,
  imports: [
    CommonModule,
    MatCardModule,
    MatTableModule,
    MatIconModule,
    MatButtonModule,
    MatProgressSpinnerModule,
    MatSnackBarModule,
    NavbarComponent
  ],
  templateUrl: './rewards.component.html',
  styleUrls: ['./rewards.component.scss']
})
export class RewardsComponent implements OnInit, AfterViewInit {
  @ViewChild('rewardsCard', { static: false }) rewardsCard!: ElementRef;

  availablePoints = 0;
  totalEarned = 0;
  totalRedeemed = 0;
  rewards: RewardResponse[] = [];
  redemptions: RewardRedemptionResponse[] = [];
  displayedColumns: string[] = ['date', 'transactionId', 'amount', 'points'];
  redemptionColumns: string[] = ['date', 'transactionId', 'points'];
  loading = true;
  noRewards = false;

  constructor(
    private rewardService: RewardService,
    private snackBar: MatSnackBar,
    private animationsService: AnimationsService,
    private router: Router
  ) {}

  ngOnInit(): void {
    this.loadRewards();
  }

  loadRewards(): void {
    this.loading = true;
    this.rewardService.getMyRewards().subscribe({
      next: (summary) => {
        this.availablePoints = summary.availablePoints;
        this.totalEarned = summary.totalEarned;
        this.totalRedeemed = summary.totalRedeemed;
        this.rewards = summary.history;
        this.redemptions = summary.redemptions;
        this.loading = false;
        this.noRewards = summary.history.length === 0 && summary.redemptions.length === 0;
        setTimeout(() => this.animateTableRows(), 100);
      },
      error: () => {
        this.loading = false;
        this.snackBar.open('Failed to load rewards', 'Close', {
          duration: 3000,
          panelClass: ['error-snackbar']
        });
      }
    });
  }

  ngAfterViewInit(): void {
    if (this.rewardsCard) {
      this.animationsService.fadeIn(this.rewardsCard.nativeElement, 0.6);
    }
  }

  animateTableRows(): void {
    const rows = document.querySelectorAll('.rewards-table tbody tr');
    if (rows.length > 0) {
      this.animationsService.staggerFadeIn(Array.from(rows) as HTMLElement[], 0.08);
    }
  }

  formatDate(dateString: string): string {
    return new Date(dateString).toLocaleDateString('en-IN', {
      year: 'numeric', month: 'short', day: 'numeric',
      hour: '2-digit', minute: '2-digit'
    });
  }

  refreshRewards(): void {
    this.loadRewards();
    this.snackBar.open('Rewards refreshed', 'Close', {
      duration: 2000,
      panelClass: ['success-snackbar']
    });
  }

  goBack(): void {
    this.router.navigate(['/dashboard']);
  }
}

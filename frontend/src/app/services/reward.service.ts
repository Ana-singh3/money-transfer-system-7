import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { map } from 'rxjs/operators';
import { environment } from '../../environments/environment';
import { RewardSummaryResponse } from '../models/reward.model';

@Injectable({
  providedIn: 'root'
})
export class RewardService {
  private apiUrl = environment.apiUrl;

  constructor(private http: HttpClient) { }

  getMyRewards(): Observable<RewardSummaryResponse> {
    return this.http.get<any>(`${this.apiUrl}/rewards/me`).pipe(
      map(response => ({
        availablePoints: response.availablePoints,
        totalEarned: response.totalEarned,
        totalRedeemed: response.totalRedeemed,
        history: (response.history || []).map((r: any) => ({
          rewardId: r.rewardId,
          transactionId: r.transactionId,
          points: r.points,
          transactionAmount: typeof r.transactionAmount === 'string'
            ? parseFloat(r.transactionAmount) : r.transactionAmount,
          grantedOn: r.grantedOn
        })),
        redemptions: (response.redemptions || []).map((r: any) => ({
          redemptionId: r.redemptionId,
          transactionId: r.transactionId,
          pointsUsed: r.pointsUsed,
          rupeeValue: typeof r.rupeeValue === 'string'
            ? parseFloat(r.rupeeValue) : r.rupeeValue,
          redeemedOn: r.redeemedOn
        }))
      }))
    );
  }
}

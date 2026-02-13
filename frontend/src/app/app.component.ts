import { Component } from '@angular/core';
import { RouterOutlet } from '@angular/router';
import { trigger, transition, style, animate, query } from '@angular/animations';

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [RouterOutlet],
  template: `
    <div class="app-root">
      <div class="route-wrap" [@routeAnimations]="getRouteAnimationData()">
        <router-outlet></router-outlet>
      </div>
      <footer class="app-footer">© {{ currentYear }} MoneyFlow. All rights reserved.</footer>
    </div>
  `,
  styles: [
    `
      .app-root { display: flex; min-height: 100vh; flex-direction: column; }
      .route-wrap { flex: 1 1 auto; }
      .app-footer {
        flex-shrink: 0;
        text-align: center;
        padding: 10px 12px;
        font-size: 0.875rem;
        color: #64748b;
        background: transparent;
      }
      @media (max-width: 600px) {
        .app-footer { font-size: 0.78rem; padding: 8px 10px; }
      }
    `
  ],
  animations: [
    trigger('routeAnimations', [
      transition('* <=> *', [
        query(':enter, :leave', [
          style({
            position: 'absolute',
            left: 0,
            width: '100%',
            opacity: 0,
            transform: 'translateY(20px)'
          })
        ], { optional: true }),
        query(':leave', animate('300ms ease-out', style({ opacity: 0, transform: 'translateY(-20px)' })), { optional: true }),
        query(':enter', animate('400ms ease-out', style({ opacity: 1, transform: 'translateY(0)' })), { optional: true })
      ])
    ])
  ]
})
export class AppComponent {
  title = 'money-transfer-app';
  currentYear = new Date().getFullYear();

  getRouteAnimationData() {
    return 'route';
  }
}
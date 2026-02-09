import { Component } from '@angular/core';
import { RouterOutlet } from '@angular/router';
import { trigger, transition, style, animate, query } from '@angular/animations';

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [RouterOutlet],
  template: '<div [@routeAnimations]="getRouteAnimationData()"><router-outlet></router-outlet></div>',
  styles: [`
    div {
      position: relative;
      width: 100%;
      height: 100%;
    }
  `],
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

  getRouteAnimationData() {
    return 'route';
  }
}
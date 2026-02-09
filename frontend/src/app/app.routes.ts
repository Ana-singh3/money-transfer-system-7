import { Routes } from '@angular/router';
import { AuthGuard } from './guards/auth.guard';
import { AdminGuard } from './guards/admin.guard';

export const routes: Routes = [
  { 
    path: '', 
    redirectTo: '/auth', 
    pathMatch: 'full' 
  },
  { 
    path: 'auth', 
    loadComponent: () => import('./components/auth/auth.component').then(m => m.AuthComponent)
  },
  { 
    path: 'login', 
    redirectTo: '/auth',
    pathMatch: 'full'
  },
  { 
    path: 'dashboard', 
    loadComponent: () => import('./components/dashboard/dashboard.component').then(m => m.DashboardComponent),
    canActivate: [AuthGuard]
  },
  { 
    path: 'transfer', 
    loadComponent: () => import('./components/transfer/transfer.component').then(m => m.TransferComponent),
    canActivate: [AuthGuard]
  },
  { 
    path: 'history', 
    loadComponent: () => import('./components/history/history.component').then(m => m.HistoryComponent),
    canActivate: [AuthGuard]
  },
  {
    path: 'admin',
    children: [
      {
        path: 'dashboard',
        loadComponent: () => import('./components/admin-dashboard/admin-dashboard.component').then(m => m.AdminDashboardComponent),
        canActivate: [AdminGuard]
      },
      {
        path: 'account/:id',
        loadComponent: () => import('./components/admin-account-detail/admin-account-detail.component').then(m => m.AdminAccountDetailComponent),
        canActivate: [AdminGuard]
      }
    ]
  },
  { 
    path: '**', 
    redirectTo: '/auth' 
  }
];
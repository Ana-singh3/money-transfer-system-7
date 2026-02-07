import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { BehaviorSubject, Observable, of } from 'rxjs';
import { map, delay } from 'rxjs/operators';
import { environment } from '../../environments/environment';
import { LoginRequest, LoginResponse, User } from '../models/auth.model';

@Injectable({
  providedIn: 'root'
})
export class AuthService {
  private currentUserSubject: BehaviorSubject<User | null>;
  public currentUser: Observable<User | null>;
  private apiUrl = environment.apiUrl;

  // Mock mode flag
  private mockMode = true; // Set to true for demo mode

  constructor(private http: HttpClient) {
    const storedUser = localStorage.getItem('currentUser');
    this.currentUserSubject = new BehaviorSubject<User | null>(
      storedUser ? JSON.parse(storedUser) : null
    );
    this.currentUser = this.currentUserSubject.asObservable();
  }

  public get currentUserValue(): User | null {
    return this.currentUserSubject.value;
  }

  login(username: string, password: string): Observable<LoginResponse> {
    // MOCK MODE - Remove this when backend is ready
    if (this.mockMode) {
      return this.mockLogin(username, password);
    }

    // Real API call
    return this.http.post<LoginResponse>(`${this.apiUrl}/auth/login`, { username, password })
      .pipe(map(response => {
        const user: User = {
          accountId: response.accountId,
          holderName: response.holderName,
          token: response.token
        };
        localStorage.setItem('currentUser', JSON.stringify(user));
        this.currentUserSubject.next(user);
        return response;
      }));
  }

  // Mock login for demo
  private mockLogin(username: string, password: string): Observable<LoginResponse> {
    // Accept any username/password for demo
    const mockResponse: LoginResponse = {
      token: 'mock-jwt-token-12345',
      accountId: 1001,
      holderName: username || 'Demo User'
    };

    const user: User = {
      accountId: mockResponse.accountId,
      holderName: mockResponse.holderName,
      token: mockResponse.token
    };

    localStorage.setItem('currentUser', JSON.stringify(user));
    this.currentUserSubject.next(user);

    // Simulate API delay
    return of(mockResponse).pipe(delay(500));
  }

  logout(): void {
    localStorage.removeItem('currentUser');
    this.currentUserSubject.next(null);
  }

  isAuthenticated(): boolean {
    return !!this.currentUserValue;
  }

  getToken(): string | null {
    return this.currentUserValue?.token || null;
  }

  getAccountId(): number | null {
    return this.currentUserValue?.accountId || null;
  }
}
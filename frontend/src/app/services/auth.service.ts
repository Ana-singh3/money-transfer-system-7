import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { BehaviorSubject, Observable, of } from 'rxjs';
import { map, switchMap } from 'rxjs/operators';
import { environment } from '../../environments/environment';
import { LoginRequest, LoginResponse, RegisterRequest, RegisterResponse, User } from '../models/auth.model';
import { AccountResponse } from '../models/account.model';

@Injectable({
  providedIn: 'root'
})
export class AuthService {
  private currentUserSubject: BehaviorSubject<User | null>;
  public currentUser: Observable<User | null>;
  private apiUrl = environment.apiUrl;

  constructor(private http: HttpClient) {
    const storedUser = localStorage.getItem('currentUser');
    const storedToken = localStorage.getItem('jwt_token');
    
    let user: User | null = null;
    if (storedUser) {
      try {
        const parsedUser = JSON.parse(storedUser) as User;
        if (!parsedUser.token && storedToken) {
          parsedUser.token = storedToken;
        }
        user = parsedUser;
      } catch (e) {
        localStorage.removeItem('currentUser');
      }
    }
    
    this.currentUserSubject = new BehaviorSubject<User | null>(user);
    this.currentUser = this.currentUserSubject.asObservable();
  }

  public get currentUserValue(): User | null {
    return this.currentUserSubject.value;
  }

  login(username: string, password: string): Observable<LoginResponse> {
    const loginRequest: LoginRequest = { username, password };
    const loginUrl = `${this.apiUrl}/auth/login`;
    
    return this.http.post<LoginResponse>(loginUrl, loginRequest)
      .pipe(
        switchMap((loginResponse: LoginResponse) => {
          localStorage.setItem('jwt_token', loginResponse.token);
          
          if (loginResponse.accountId) {
            return this.http.get<AccountResponse>(`${this.apiUrl}/accounts/${loginResponse.accountId}`).pipe(
              map((account: AccountResponse) => {
                const user: User = {
                  username: loginResponse.username,
                  role: loginResponse.role,
                  token: loginResponse.token,
                  accountId: account.accountId,
                  holderName: account.holderName
                };
                
                localStorage.setItem('currentUser', JSON.stringify(user));
                this.currentUserSubject.next(user);
                return loginResponse;
              })
            );
          } else {
            const user: User = {
              username: loginResponse.username,
              role: loginResponse.role,
              token: loginResponse.token
            };
            localStorage.setItem('currentUser', JSON.stringify(user));
            this.currentUserSubject.next(user);
            return of(loginResponse);
          }
        })
      );
  }

  signup(username: string, password: string): Observable<RegisterResponse> {
    const signupRequest: RegisterRequest = { username, password };
    const signupUrl = `${this.apiUrl}/auth/signup`;
    return this.http.post<RegisterResponse>(signupUrl, signupRequest);
  }

  logout(): void {
    localStorage.removeItem('currentUser');
    localStorage.removeItem('jwt_token');
    this.currentUserSubject.next(null);
  }

  isAuthenticated(): boolean {
    return !!this.currentUserValue && !!this.getToken();
  }

  getToken(): string | null {
    const user = this.currentUserValue;
    if (user?.token) {
      return user.token;
    }
    
    const storedToken = localStorage.getItem('jwt_token');
    if (storedToken && user) {
      user.token = storedToken;
      localStorage.setItem('currentUser', JSON.stringify(user));
      this.currentUserSubject.next(user);
      return storedToken;
    }
    
    return storedToken;
  }

  getAccountId(): string | null {
    return this.currentUserValue?.accountId || null;
  }

  refreshUserAccount(): Observable<AccountResponse> {
    const currentUser = this.currentUserValue;
    if (!currentUser?.accountId) {
      throw new Error('No account ID available');
    }
    
    return this.http.get<AccountResponse>(`${this.apiUrl}/accounts/${currentUser.accountId}`).pipe(
      map((account: AccountResponse) => {
        if (currentUser) {
          const updatedUser: User = {
            ...currentUser,
            accountId: account.accountId,
            holderName: account.holderName
          };
          localStorage.setItem('currentUser', JSON.stringify(updatedUser));
          this.currentUserSubject.next(updatedUser);
        }
        return account;
      })
    );
  }
}

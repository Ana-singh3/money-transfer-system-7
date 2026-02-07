export interface LoginRequest {
  username: string;
  password: string;
}

export interface LoginResponse {
  token: string;
  accountId: number;
  holderName: string;
}

export interface User {
  accountId: number;
  holderName: string;
  token: string;
}
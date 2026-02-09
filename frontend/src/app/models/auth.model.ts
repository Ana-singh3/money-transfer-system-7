export interface LoginRequest {
  username: string;
  password: string;
}

export interface LoginResponse {
  token: string;
  username: string;
  role: string;
  accountId: string;
}

export interface RegisterRequest {
  username: string;
  password: string;
}

export interface RegisterResponse {
  message: string;
  username: string;
  role: string;
}

export interface User {
  username: string;
  role: string;
  token: string;
  accountId?: string;
  holderName?: string;
}
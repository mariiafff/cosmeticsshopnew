import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, tap } from 'rxjs';

import { environment } from '../../environments/environment';

export interface LoginRequest {
  email: string;
  password: string;
}

export interface RegisterRequest {
  name: string;
  email: string;
  password: string;
  [key: string]: unknown;
}

export interface AuthResponse {
  token: string;
  type?: string;
  email?: string;
  role?: string;
}

export interface JwtPayload {
  sub?: string;
  email?: string;
  role?: string;
  roles?: string[];
  exp?: number;
  [key: string]: unknown;
}

@Injectable({
  providedIn: 'root'
})
export class AuthService {
  private readonly http = inject(HttpClient);
  private readonly apiUrl = `${environment.apiBaseUrl}/auth`;
  private readonly tokenKey = 'auth_token';

  login(payload: LoginRequest): Observable<AuthResponse> {
    return this.http
      .post<AuthResponse>(`${this.apiUrl}/login`, payload)
      .pipe(tap((response) => this.saveToken(response.token)));
  }

  register(payload: RegisterRequest): Observable<AuthResponse> {
    return this.http
      .post<AuthResponse>(`${this.apiUrl}/register`, payload)
      .pipe(tap((response) => this.saveToken(response.token)));
  }

  logout(): void {
    localStorage.removeItem(this.tokenKey);
  }

  getToken(): string | null {
    return localStorage.getItem(this.tokenKey);
  }

  getRole(): string | null {
    const token = this.getToken();
    if (!token) {
      return null;
    }

    const payload = this.decodeJwt(token);
    const roleClaim = payload.role ?? payload.roles;

    if (Array.isArray(roleClaim)) {
      return roleClaim.length ? String(roleClaim[0]) : null;
    }

    return roleClaim ? String(roleClaim) : null;
  }

  isLoggedIn(): boolean {
    return !!this.getToken();
  }

  private saveToken(token: string): void {
    localStorage.setItem(this.tokenKey, token);
  }

  private decodeJwt(token: string): JwtPayload {
    const parts = token.split('.');
    if (parts.length !== 3) {
      return {};
    }

    try {
      const payloadJson = atob(this.padBase64(parts[1]));
      return JSON.parse(payloadJson) as JwtPayload;
    } catch {
      return {};
    }
  }

  private padBase64(base64: string): string {
    const padding = 4 - (base64.length % 4);
    return padding < 4 ? `${base64}${'='.repeat(padding)}` : base64;
  }
}

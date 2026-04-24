import { Injectable, computed, inject, signal } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, tap } from 'rxjs';

import { environment } from '../../environments/environment';

export interface LoginRequest {
  email: string;
  password: string;
}

export interface RegisterRequest {
  firstName: string;
  lastName: string;
  email: string;
  password: string;
  role: string;
  city?: string;
  membershipType?: string;
}

export interface AuthResponse {
  token: string;
  refreshToken?: string;
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
  private readonly refreshTokenKey = 'refresh_token';
  private readonly tokenSignal = signal<string | null>(this.readToken());

  readonly isAuthenticated = computed(() => !!this.tokenSignal() && !this.isTokenExpired());
  readonly currentRole = computed(() => this.extractRole(this.tokenSignal()));
  readonly currentEmail = computed(() => this.extractEmail(this.tokenSignal()));

  login(payload: LoginRequest): Observable<AuthResponse> {
    return this.http
      .post<AuthResponse>(`${this.apiUrl}/login`, payload)
      .pipe(tap((response) => this.saveAuth(response)));
  }

  register(payload: RegisterRequest): Observable<AuthResponse> {
    return this.http
      .post<AuthResponse>(`${this.apiUrl}/register`, payload)
      .pipe(tap((response) => this.saveAuth(response)));
  }

  logout(): void {
    if (!this.hasBrowserStorage()) {
      this.tokenSignal.set(null);
      return;
    }
    localStorage.removeItem(this.tokenKey);
    localStorage.removeItem(this.refreshTokenKey);
    this.tokenSignal.set(null);
  }

  getToken(): string | null {
    return this.tokenSignal();
  }

  getRole(): string | null {
    return this.currentRole();
  }

  isLoggedIn(): boolean {
    return this.isAuthenticated();
  }

  getUserEmail(): string | null {
    return this.currentEmail();
  }

  private saveAuth(response: AuthResponse): void {
    if (!this.hasBrowserStorage()) {
      this.tokenSignal.set(response.token);
      return;
    }
    localStorage.setItem(this.tokenKey, response.token);
    if (response.refreshToken) {
      localStorage.setItem(this.refreshTokenKey, response.refreshToken);
    }
    this.tokenSignal.set(response.token);
  }

  private hasBrowserStorage(): boolean {
    return typeof localStorage !== 'undefined';
  }

  private isTokenExpired(): boolean {
    const token = this.tokenSignal();
    if (!token) {
      return true;
    }

    const payload = this.decodeJwt(token);
    const exp = typeof payload.exp === 'number' ? payload.exp : null;
    if (!exp) {
      return false;
    }

    return Date.now() >= exp * 1000;
  }

  private readToken(): string | null {
    if (!this.hasBrowserStorage()) {
      return null;
    }
    return localStorage.getItem(this.tokenKey);
  }

  private extractEmail(token: string | null): string | null {
    if (!token) {
      return null;
    }

    const payload = this.decodeJwt(token);
    return typeof payload.sub === 'string' ? payload.sub : null;
  }

  private extractRole(token: string | null): string | null {
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

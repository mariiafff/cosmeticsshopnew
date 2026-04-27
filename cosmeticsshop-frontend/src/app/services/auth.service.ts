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
  accountType: string;
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
  name?: string;
  role?: string;
  roles?: string[];
  exp?: number;
  [key: string]: unknown;
}

export interface AuthUser {
  email: string;
  name: string | null;
  role: string | null;
}

@Injectable({
  providedIn: 'root'
})
export class AuthService {
  private readonly http = inject(HttpClient);
  private readonly apiUrl = `${environment.apiBaseUrl}/auth`;
  private readonly tokenKey = 'auth_token';
  private readonly refreshTokenKey = 'refresh_token';
  private readonly checkoutAuthTokenKey = 'luime_checkout_auth_token';
  private readonly checkoutRefreshTokenKey = 'luime_checkout_refresh_token';
  private readonly tokenSignal = signal<string | null>(this.readToken());

  readonly isAuthenticated = computed(() => this.isLoggedIn());
  readonly currentRole = computed(() => this.getUser()?.role ?? null);
  readonly currentEmail = computed(() => this.getUser()?.email ?? null);

  login(payload: LoginRequest): Observable<AuthResponse> {
    const normalizedPayload = {
      email: payload.email?.trim().toLowerCase() ?? '',
      password: payload.password?.trim() ?? ''
    };

    return this.http
      .post<AuthResponse>(`${this.apiUrl}/login`, normalizedPayload)
      .pipe(tap((response) => this.saveAuth(response)));
  }

  register(payload: RegisterRequest): Observable<AuthResponse> {
    const normalizedPayload = {
      ...payload,
      email: payload.email?.trim().toLowerCase(),
      password: payload.password?.trim()
    };

    return this.http.post<AuthResponse>(`${this.apiUrl}/register`, normalizedPayload);
  }

  logout(): void {
    this.clearAuth();
  }

  getToken(): string | null {
    return this.ensureTokenAvailable();
  }

  getRole(): string | null {
    return this.getUser()?.role ?? null;
  }

  isLoggedIn(): boolean {
    return this.getUser() !== null;
  }

  getUser(): AuthUser | null {
    const token = this.ensureTokenAvailable();
    if (!token) {
      return null;
    }

    const payload = this.decodeJwt(token);
    const email = this.extractEmailFromPayload(payload);
    if (!email) {
      this.clearAuth();
      return null;
    }

    const exp = typeof payload.exp === 'number' ? payload.exp : null;
    if (exp !== null && Date.now() >= exp * 1000) {
      this.clearAuth();
      return null;
    }

    return {
      email,
      name: this.extractNameFromPayload(payload),
      role: this.extractRoleFromPayload(payload)
    };
  }

  getUserEmail(): string | null {
    return this.getUser()?.email ?? null;
  }

  restoreCheckoutTokens(): boolean {
    const restoredToken = this.restoreTokenFromCheckoutBackup();
    if (!restoredToken || !this.isValidToken(restoredToken)) {
      return false;
    }

    this.tokenSignal.set(restoredToken);
    return true;
  }

  private saveAuth(response: AuthResponse): void {
    if (!response.token || !this.isValidToken(response.token)) {
      this.clearAuth();
      return;
    }

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

  private ensureTokenAvailable(): string | null {
    const currentToken = this.tokenSignal();
    if (currentToken && this.isValidToken(currentToken)) {
      return currentToken;
    }

    const storageToken = this.readToken();
    if (storageToken) {
      this.tokenSignal.set(storageToken);
      return storageToken;
    }

    return null;
  }

  private isTokenExpired(): boolean {
    return this.getUser() === null;
  }

  private readToken(): string | null {
    if (!this.hasBrowserStorage()) {
      return null;
    }

    let token = localStorage.getItem(this.tokenKey);
    if (token && !this.isValidToken(token)) {
      this.clearAuth();
      token = null;
    }

    if (!token) {
      token = this.restoreTokenFromCheckoutBackup();
    }

    return token;
  }

  private restoreTokenFromCheckoutBackup(): string | null {
    if (typeof sessionStorage === 'undefined') {
      return null;
    }

    const token = sessionStorage.getItem(this.checkoutAuthTokenKey);
    if (!token || !this.isValidToken(token)) {
      return null;
    }

    localStorage.setItem(this.tokenKey, token);

    const refreshToken = sessionStorage.getItem(this.checkoutRefreshTokenKey);
    if (refreshToken) {
      localStorage.setItem(this.refreshTokenKey, refreshToken);
    }

    return token;
  }

  private extractEmailFromPayload(payload: JwtPayload): string | null {
    if (typeof payload.email === 'string' && payload.email.trim()) {
      return payload.email;
    }
    if (typeof payload.sub === 'string' && payload.sub.trim()) {
      return payload.sub;
    }
    return null;
  }

  private extractRoleFromPayload(payload: JwtPayload): string | null {
    const roleClaim = payload.role ?? payload.roles;

    if (Array.isArray(roleClaim)) {
      return roleClaim.length ? String(roleClaim[0]) : null;
    }

    return roleClaim ? String(roleClaim) : null;
  }

  private extractNameFromPayload(payload: JwtPayload): string | null {
    if (typeof payload.name === 'string' && payload.name.trim()) {
      return payload.name.trim();
    }

    const email = this.extractEmailFromPayload(payload);
    if (!email) {
      return null;
    }

    const base = email.split('@')[0].split(/[._-]/)[0];
    return base ? `${base.charAt(0).toUpperCase()}${base.slice(1)}` : null;
  }

  private isValidToken(token: string): boolean {
    const payload = this.decodeJwt(token);
    const email = this.extractEmailFromPayload(payload);
    if (!email) {
      return false;
    }

    if (typeof payload.exp === 'number' && Date.now() >= payload.exp * 1000) {
      return false;
    }

    return true;
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

  private clearAuth(): void {
    if (this.hasBrowserStorage()) {
      localStorage.removeItem(this.tokenKey);
      localStorage.removeItem(this.refreshTokenKey);
    }
    this.tokenSignal.set(null);
  }
}

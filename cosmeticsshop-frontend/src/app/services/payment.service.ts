import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

import { environment } from '../../environments/environment';
import { CartItem } from './cart.service';
import { CheckoutRequest } from './order.service';

export interface CheckoutSessionResponse {
  url: string;
}

@Injectable({
  providedIn: 'root'
})
export class PaymentService {
  private readonly http = inject(HttpClient);
  private readonly apiUrl = `${environment.apiBaseUrl}/payments`;
  private readonly pendingCheckoutKey = 'luime_pending_checkout';
  private readonly checkoutAuthTokenKey = 'luime_checkout_auth_token';
  private readonly checkoutRefreshTokenKey = 'luime_checkout_refresh_token';

  createCheckoutSession(items: CartItem[]): Observable<CheckoutSessionResponse> {
    const origin = typeof window === 'undefined' ? 'http://127.0.0.1:4200' : window.location.origin;
    return this.http.post<CheckoutSessionResponse>(`${this.apiUrl}/checkout`, {
      items,
      successUrl: `${origin}/checkout/payment-success`,
      cancelUrl: `${origin}/checkout?payment=cancelled`
    });
  }

  rememberPendingCheckout(items: CartItem[], paymentMethod = 'CARD'): void {
    if (typeof sessionStorage === 'undefined') {
      return;
    }

    const payload: CheckoutRequest = {
      items: items.map((item) => ({
        productId: item.productId,
        quantity: item.quantity
      })),
      paymentMethod
    };

    sessionStorage.setItem(this.pendingCheckoutKey, JSON.stringify(payload));
    this.backupAuthTokens();
  }

  getPendingCheckoutRequest(): CheckoutRequest | null {
    if (typeof sessionStorage === 'undefined') {
      return null;
    }

    try {
      const raw = sessionStorage.getItem(this.pendingCheckoutKey);
      return raw ? (JSON.parse(raw) as CheckoutRequest) : null;
    } catch {
      return null;
    }
  }

  clearPendingCheckout(): void {
    if (typeof sessionStorage === 'undefined') {
      return;
    }

    sessionStorage.removeItem(this.pendingCheckoutKey);
  }

  clearCheckoutAuthBackup(): void {
    if (typeof sessionStorage === 'undefined') {
      return;
    }

    sessionStorage.removeItem(this.checkoutAuthTokenKey);
    sessionStorage.removeItem(this.checkoutRefreshTokenKey);
  }

  private backupAuthTokens(): void {
    if (typeof localStorage === 'undefined') {
      return;
    }

    const authToken = localStorage.getItem('auth_token');
    const refreshToken = localStorage.getItem('refresh_token');

    if (authToken) {
      sessionStorage.setItem(this.checkoutAuthTokenKey, authToken);
    }

    if (refreshToken) {
      sessionStorage.setItem(this.checkoutRefreshTokenKey, refreshToken);
    }
  }
}

import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

import { environment } from '../../environments/environment';
import { CartItem } from './cart.service';

export interface CheckoutSessionResponse {
  url: string;
}

@Injectable({
  providedIn: 'root'
})
export class PaymentService {
  private readonly http = inject(HttpClient);
  private readonly apiUrl = `${environment.apiBaseUrl}/payments`;

  createCheckoutSession(items: CartItem[]): Observable<CheckoutSessionResponse> {
    const origin = typeof window === 'undefined' ? 'http://127.0.0.1:4200' : window.location.origin;
    return this.http.post<CheckoutSessionResponse>(`${this.apiUrl}/checkout`, {
      items,
      successUrl: `${origin}/orders/payment-success?payment=success`,
      cancelUrl: `${origin}/checkout?payment=cancelled`
    });
  }
}

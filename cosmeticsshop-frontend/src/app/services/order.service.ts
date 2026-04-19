import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

import { environment } from '../../environments/environment';

export interface Order {
  id: number;
  productId: number;
  quantity: number;
  totalPrice?: number;
  status?: string;
  createdAt?: string;
}

export interface CreateOrderRequest {
  productId: number;
  quantity: number;
}

@Injectable({
  providedIn: 'root'
})
export class OrderService {
  private readonly http = inject(HttpClient);
  private readonly apiUrl = `${environment.apiBaseUrl}/orders`;

  getMyOrders(): Observable<Order[]> {
    return this.http.get<Order[]>(`${this.apiUrl}/my`);
  }

  createOrder(payload: CreateOrderRequest): Observable<Order> {
    return this.http.post<Order>(this.apiUrl, payload);
  }
}

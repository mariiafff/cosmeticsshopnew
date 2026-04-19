import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

import { environment } from '../../environments/environment';

export interface Order {
  id: number;
  orderNumber?: string;
  totalPrice?: number;
  status?: string;
  shipmentStatus?: string;
  paymentMethod?: string;
  createdAt?: string;
}

export interface CreateOrderRequest {
  productId: number;
  quantity: number;
  paymentMethod?: string;
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

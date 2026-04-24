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
  items?: OrderItem[];
}

export interface OrderItem {
  productId: number;
  productName: string;
  category?: string;
  quantity: number;
  price: number;
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

  getAllOrders(): Observable<Order[]> {
    return this.http.get<Order[]>(this.apiUrl);
  }

  createOrder(payload: CreateOrderRequest): Observable<Order> {
    return this.http.post<Order>(this.apiUrl, payload);
  }

  updateOrder(id: number, payload: Order): Observable<Order> {
    return this.http.put<Order>(`${this.apiUrl}/${id}`, payload);
  }

  deleteOrder(id: number): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/${id}`);
  }
}

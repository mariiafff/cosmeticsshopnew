import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

import { environment } from '../../environments/environment';

export interface ProductCategory {
  id: number;
  name: string;
}

export interface ProductStore {
  id: number;
  name: string;
}

export interface Product {
  id: number;
  name: string;
  description?: string;
  category?: ProductCategory;
  price?: number;
  stockQuantity?: number;
  sku?: string;
  store?: ProductStore;
  status?: string;
  averageRating?: number;
}

export interface CreateProductRequest {
  name: string;
  description?: string;
  price: number;
  sku?: string;
  stockQuantity?: number;
  status?: string;
  store?: { id: number };
  category?: { id: number };
}

@Injectable({
  providedIn: 'root'
})
export class ProductsService {
  private readonly http = inject(HttpClient);
  private readonly apiUrl = `${environment.apiBaseUrl}/products`;

  getProducts(query = ''): Observable<Product[]> {
    const suffix = query.trim() ? `?q=${encodeURIComponent(query.trim())}` : '';
    return this.http.get<Product[]>(`${this.apiUrl}${suffix}`);
  }

  createProduct(payload: CreateProductRequest): Observable<Product> {
    return this.http.post<Product>(this.apiUrl, payload);
  }

  updateProduct(id: number, payload: CreateProductRequest): Observable<Product> {
    return this.http.put<Product>(`${this.apiUrl}/${id}`, payload);
  }

  deleteProduct(id: number): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/${id}`);
  }
}

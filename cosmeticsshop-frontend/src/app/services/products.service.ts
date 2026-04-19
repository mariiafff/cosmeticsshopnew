import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

import { environment } from '../../environments/environment';

export interface Product {
  id: number;
  name: string;
  description: string;
  category?: string;
  price?: number;
  stockQuantity?: number;
  sku?: string;
  storeId?: number;
  averageRating?: number;
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
}

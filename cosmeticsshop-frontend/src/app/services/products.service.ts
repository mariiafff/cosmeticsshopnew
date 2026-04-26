import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable, map } from 'rxjs';

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
  unitPrice?: number;
  stockQuantity?: number;
  sku?: string;
  store?: ProductStore;
  status?: string;
  averageRating?: number;
  stockCode?: string;
  currencyCode?: string;
  createdAt?: string;
  updatedAt?: string;
}

export interface ProductsQuery {
  page?: number;
  size?: number;
  search?: string;
  sort?: string;
}

export interface ProductsPage {
  content: Product[];
  number: number;
  size: number;
  totalElements: number;
  totalPages: number;
  numberOfElements: number;
  first: boolean;
  last: boolean;
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

  getProductsPage(query: ProductsQuery = {}): Observable<ProductsPage> {
    let params = new HttpParams();

    if (query.page !== undefined) {
      params = params.set('page', query.page);
    }
    if (query.size !== undefined) {
      params = params.set('size', query.size);
    }
    if (query.search?.trim()) {
      params = params.set('search', query.search.trim());
    }
    if (query.sort?.trim()) {
      params = params.set('sort', query.sort.trim());
    }

    return this.http.get<ProductsPage>(this.apiUrl, { params });
  }

  getProducts(query: ProductsQuery = {}): Observable<Product[]> {
    return this.getProductsPage(query).pipe(map((response) => response.content));
  }

  getAllProducts(size = 5000): Observable<Product[]> {
    return this.getProducts({ page: 0, size, sort: 'name,asc' });
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

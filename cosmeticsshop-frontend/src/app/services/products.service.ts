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
  categoryId?: number;
  categoryName?: string;
  category?: ProductCategory;
  price?: number;
  unitPrice?: number;
  stockQuantity?: number;
  sku?: string;
  storeId?: number;
  storeName?: string;
  store?: ProductStore;
  status?: string;
  averageRating?: number;
  stockCode?: string;
  currencyCode?: string;
  imageUrl?: string;
  image_url?: string;
  createdAt?: string;
  updatedAt?: string;
}

interface ProductApiDto {
  id: number;
  name: string;
  description?: string;
  price?: number;
  stockQuantity?: number;
  sku?: string;
  status?: string;
  imageUrl?: string;
  storeId?: number;
  storeName?: string;
  categoryId?: number;
  categoryName?: string;
}

export interface ProductsQuery {
  page?: number;
  size?: number;
  search?: string;
  sort?: string;
  categoryId?: number;
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
    if (query.categoryId !== undefined) {
      params = params.set('categoryId', query.categoryId);
    }

    return this.http.get<ProductsPageApi>(this.apiUrl, { params }).pipe(
      map((response) => ({
        ...response,
        content: response.content.map((product) => this.mapProduct(product))
      }))
    );
  }

  getProducts(query: ProductsQuery = {}): Observable<Product[]> {
    return this.getProductsPage(query).pipe(map((response) => response.content));
  }

  getAllProducts(size = 5000): Observable<Product[]> {
    return this.getProducts({ page: 0, size, sort: 'name,asc' });
  }

  getManageProducts(size = 5000): Observable<Product[]> {
    const params = new HttpParams()
      .set('page', 0)
      .set('size', size)
      .set('sort', 'name,asc');

    return this.http
      .get<ProductsPageApi>(`${this.apiUrl}/manage`, { params })
      .pipe(map((response) => response.content.map((product) => this.mapProduct(product))));
  }

  createProduct(payload: CreateProductRequest): Observable<Product> {
    return this.http.post<ProductApiDto>(this.apiUrl, payload).pipe(map((product) => this.mapProduct(product)));
  }

  updateProduct(id: number, payload: CreateProductRequest): Observable<Product> {
    return this.http.put<ProductApiDto>(`${this.apiUrl}/${id}`, payload).pipe(map((product) => this.mapProduct(product)));
  }

  deleteProduct(id: number): Observable<Product> {
    return this.http.delete<ProductApiDto>(`${this.apiUrl}/${id}`).pipe(map((product) => this.mapProduct(product)));
  }

  activateProduct(id: number): Observable<Product> {
    return this.http.patch<ProductApiDto>(`${this.apiUrl}/${id}/activate`, {}).pipe(map((product) => this.mapProduct(product)));
  }

  private mapProduct(product: ProductApiDto): Product {
    return {
      ...product,
      category: product.categoryId || product.categoryName
        ? {
            id: product.categoryId ?? 0,
            name: product.categoryName ?? 'Uncategorized'
          }
        : undefined,
      store: product.storeId || product.storeName
        ? {
            id: product.storeId ?? 0,
            name: product.storeName ?? 'Not assigned'
          }
        : undefined
    };
  }
}
interface ProductsPageApi {
  content: ProductApiDto[];
  number: number;
  size: number;
  totalElements: number;
  totalPages: number;
  numberOfElements: number;
  first: boolean;
  last: boolean;
}

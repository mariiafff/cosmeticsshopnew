import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

import { environment } from '../../environments/environment';

export interface Category {
  id: number;
  name: string;
  description?: string;
  parentCategoryId?: number;
}

export interface CategoryRequest {
  name: string;
  description?: string;
  parentCategoryId?: number;
}

@Injectable({
  providedIn: 'root'
})
export class CategoriesService {
  private readonly http = inject(HttpClient);
  private readonly apiUrl = `${environment.apiBaseUrl}/categories`;

  getCategories(): Observable<Category[]> {
    return this.http.get<Category[]>(this.apiUrl);
  }

  createCategory(payload: CategoryRequest): Observable<Category> {
    return this.http.post<Category>(this.apiUrl, payload);
  }

  updateCategory(id: number, payload: CategoryRequest): Observable<Category> {
    return this.http.put<Category>(`${this.apiUrl}/${id}`, payload);
  }

  deleteCategory(id: number): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/${id}`);
  }
}

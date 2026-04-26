import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, map, tap } from 'rxjs';

import { environment } from '../../environments/environment';

export interface Store {
  id: number;
  name: string;
  status?: string;
  city?: string;
  country?: string;
  description?: string;
  ownerUserId?: number;
}

export interface StoreRequest {
  name: string;
  status?: string;
  city?: string;
  country?: string;
  description?: string;
}

interface StoreApiResponse {
  content?: Store[];
}

@Injectable({
  providedIn: 'root'
})
export class StoresService {
  private readonly http = inject(HttpClient);
  private readonly apiUrl = `${environment.apiBaseUrl}/stores`;

  getStores(): Observable<Store[]> {
    return this.http.get<Store[] | StoreApiResponse>(this.apiUrl).pipe(
      tap((response) => console.log('Stores API response:', response)),
      map((response) => {
        if (Array.isArray(response)) {
          return response;
        }
        if (Array.isArray(response?.content)) {
          return response.content;
        }
        return [];
      }),
      tap((stores) => console.log('Normalized stores:', stores))
    );
  }

  createStore(payload: StoreRequest): Observable<Store> {
    return this.http.post<Store>(this.apiUrl, payload);
  }

  updateStore(id: number, payload: StoreRequest): Observable<Store> {
    return this.http.put<Store>(`${this.apiUrl}/${id}`, payload);
  }

  deleteStore(id: number): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/${id}`);
  }
}

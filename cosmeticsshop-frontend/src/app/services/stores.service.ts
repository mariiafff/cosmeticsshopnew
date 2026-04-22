import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

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

@Injectable({
  providedIn: 'root'
})
export class StoresService {
  private readonly http = inject(HttpClient);
  private readonly apiUrl = `${environment.apiBaseUrl}/stores`;

  getStores(): Observable<Store[]> {
    return this.http.get<Store[]>(this.apiUrl);
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

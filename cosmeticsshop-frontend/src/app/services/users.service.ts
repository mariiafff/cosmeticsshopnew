import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

import { environment } from '../../environments/environment';

export interface User {
  id: number;
  email: string;
  firstName?: string;
  lastName?: string;
  role?: string;
  status?: string;
  city?: string;
  membershipType?: string;
}

export interface UsersPage {
  content: User[];
  totalElements: number;
  totalPages: number;
  size: number;
  number: number;
}

@Injectable({
  providedIn: 'root'
})
export class UsersService {
  private readonly http = inject(HttpClient);
  private readonly apiUrl = `${environment.apiBaseUrl}/users`;

  getUsers(page = 0, size = 20): Observable<UsersPage> {
    return this.http.get<UsersPage>(`${this.apiUrl}?page=${page}&size=${size}`);
  }

  deleteUser(id: number): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/${id}`);
  }

  updateUserStatus(id: number, status: string): Observable<User> {
    return this.http.patch<User>(`${this.apiUrl}/${id}/status`, { status });
  }
}

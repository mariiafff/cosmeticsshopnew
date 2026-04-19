import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

import { environment } from '../../environments/environment';

export interface ChatResponse {
  question: string;
  inScope: boolean;
  sqlQuery: string;
  answer: string;
  visualizationHint: string;
  rows: Record<string, unknown>[];
}

export interface SendMessageRequest {
  question: string;
}

@Injectable({
  providedIn: 'root'
})
export class ChatService {
  private readonly http = inject(HttpClient);
  private readonly apiUrl = `${environment.apiBaseUrl}/chat`;

  getMessages(): Observable<string[]> {
    return this.http.get<string[]>(`${this.apiUrl}/messages`);
  }

  ask(payload: SendMessageRequest): Observable<ChatResponse> {
    return this.http.post<ChatResponse>(`${this.apiUrl}/ask`, payload);
  }
}

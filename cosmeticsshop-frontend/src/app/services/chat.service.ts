import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

import { environment } from '../../environments/environment';

export interface ChatResponse {
  question: string;
  generatedSql: string | null;
  rows: Record<string, unknown>[];
  message: string;
  executionTimeMs?: number;
  status: 'SUCCESS' | 'BLOCKED' | 'ERROR';
  agent: 'GUARDRAIL' | 'SQL_AGENT' | 'VALIDATOR' | 'EXECUTOR' | 'ANALYSIS' | 'ERROR';
  detectionType?: string | null;
  securityTitle?: string | null;
  securityDetails?: Record<string, unknown>;
  finalAnswer?: string | null;
  visualizationType?: 'NONE' | 'TABLE' | 'BAR' | 'LINE' | 'PIE';
  chartData?: {
    labels?: string[];
    values?: number[];
    type?: 'bar' | 'line' | 'pie';
  };
  steps?: string[];
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

  askQuestion(question: string): Observable<ChatResponse> {
    return this.http.post<ChatResponse>(`${this.apiUrl}/ask`, { question });
  }
}

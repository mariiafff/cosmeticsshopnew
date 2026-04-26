import { Component, computed, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';

import { ChatResponse, ChatService } from '../../services/chat.service';
import { AuthService } from '../../services/auth.service';

@Component({
  selector: 'app-chat-page',
  imports: [CommonModule, FormsModule, RouterLink],
  templateUrl: './chat.html',
  styleUrl: './chat.css'
})
export class ChatPage {
  private readonly chatService = inject(ChatService);
  private readonly authService = inject(AuthService);

  protected readonly response = signal<ChatResponse | null>(null);
  protected newMessage = '';
  protected readonly loading = signal(false);
  protected readonly errorMessage = signal('');
  protected readonly user = computed(() => this.authService.getUser());
  protected readonly role = computed(() => this.user()?.role ?? 'ANONYMOUS');
  protected readonly sessionLabel = computed(() => {
    const response = this.response();
    const details = response?.securityDetails ?? {};
    const role = String(details['role'] ?? this.role());
    const storeId = details['storeId'];

    if (storeId !== undefined && storeId !== null) {
      return `Aktif oturum · store_id: #${storeId} · Guardrail: Açık`;
    }
    return `Aktif oturum · ${role} · Guardrail: Açık`;
  });
  protected readonly examples = [
    'Which membership type spends the most?',
    'Which city has the most customers?',
    'Top selling products',
    'Which country generates the most revenue?'
  ];
  protected readonly pipeline = ['Ask question', 'Generate SQL', 'Validate SQL', 'Run query', 'Show results'];
  protected readonly responseColumns = computed(() => {
    const firstRow = this.response()?.rows?.[0];
    return firstRow ? Object.keys(firstRow) : [];
  });
  protected readonly chartBars = computed(() => {
    const chartData = this.response()?.chartData;
    const labels = chartData?.labels ?? [];
    const values = chartData?.values ?? [];
    const max = values.length ? Math.max(...values) : 0;

    return labels.map((label, index) => ({
      label,
      value: values[index] ?? 0,
      width: max > 0 ? `${((values[index] ?? 0) / max) * 100}%` : '0%'
    }));
  });
  protected readonly safeAlternative = computed(() => {
    const response = this.response();
    if (!response || response.status !== 'BLOCKED') {
      return '';
    }
    return response.finalAnswer ?? '';
  });

  protected sendMessage(): void {
    const text = this.newMessage.trim();
    if (!text) {
      this.errorMessage.set('Message cannot be empty.');
      return;
    }

    this.loading.set(true);
    this.errorMessage.set('');
    this.response.set(null);

    this.chatService.askQuestion(text).subscribe({
      next: (response) => {
        this.response.set(response);
        this.loading.set(false);
      },
      error: (error) => {
        this.errorMessage.set(
          error?.error?.message ?? 'The assistant could not answer that right now.'
        );
        this.loading.set(false);
      }
    });
  }

  protected runExample(example: string): void {
    this.newMessage = example;
    this.sendMessage();
  }

  protected trackByColumn(_index: number, column: string): string {
    return column;
  }

  protected trackByBar(_index: number, bar: { label: string }): string {
    return bar.label;
  }
}

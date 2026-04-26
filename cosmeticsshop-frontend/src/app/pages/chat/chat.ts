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
  protected readonly role = computed(() => this.authService.getRole() ?? 'Guest');
  protected readonly accessScope = computed(() => {
    const role = this.role().toUpperCase();
    if (role.includes('ADMIN')) {
      return 'You can ask about marketplace sales, stores, customers, reviews, orders, and products.';
    }
    if (role.includes('CORPORATE')) {
      return 'You can ask about your store sales, products, stock, reviews, customers, and orders.';
    }
    return 'You can ask about your orders, deliveries, reviews, purchase history, and favorite categories.';
  });
  protected readonly examples = [
    'Which membership type spends the most?',
    'Which city has the most customers?',
    'Top selling products',
    'Which country generates the most revenue?'
  ];
  protected readonly pipeline = ['Ask question', 'Generate SQL', 'Validate SQL', 'Run query', 'Show results'];

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

  protected responseColumns(): string[] {
    const firstRow = this.response()?.rows?.[0];
    return firstRow ? Object.keys(firstRow) : [];
  }
}

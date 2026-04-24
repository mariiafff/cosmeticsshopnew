import { Component, computed, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';

import { ChatResponse, ChatService, SendMessageRequest } from '../../services/chat.service';
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

  protected readonly tips = signal<string[]>([]);
  protected readonly responses = signal<ChatResponse[]>([]);
  protected newMessage = '';
  protected readonly isLoading = signal(false);
  protected readonly isSending = signal(false);
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
    'Which products are best sellers this month?',
    'Which items need attention?',
    'How much did I spend on my recent orders?',
    'Which category is performing best?'
  ];
  protected readonly pipeline = ['Question', 'Check access', 'Find data', 'Review answer', 'Show results'];

  constructor() {
    this.loadMessages();
  }

  protected loadMessages(): void {
    this.isLoading.set(true);
    this.errorMessage.set('');

    this.chatService.getMessages().subscribe({
      next: (messages) => {
        this.tips.set(messages);
        this.isLoading.set(false);
      },
      error: () => {
        this.errorMessage.set('The assistant is not available right now.');
        this.isLoading.set(false);
      }
    });
  }

  protected sendMessage(): void {
    const text = this.newMessage.trim();
    if (!text) {
      this.errorMessage.set('Message cannot be empty.');
      return;
    }

    this.isSending.set(true);
    this.errorMessage.set('');

    const payload: SendMessageRequest = { question: text };
    this.chatService.ask(payload).subscribe({
      next: (message) => {
        this.responses.update((messages) => [...messages, message]);
        this.newMessage = '';
        this.isSending.set(false);
      },
      error: () => {
        this.errorMessage.set('The assistant could not answer that yet.');
        this.isSending.set(false);
      }
    });
  }

  protected useExample(example: string): void {
    this.newMessage = example;
  }

  protected rowKeys(row: Record<string, unknown>): string[] {
    return Object.keys(row);
  }
}

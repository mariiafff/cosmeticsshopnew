import { Component, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';

import { ChatMessage, ChatService, SendMessageRequest } from '../../services/chat.service';

@Component({
  selector: 'app-chat-page',
  imports: [CommonModule, FormsModule, RouterLink],
  templateUrl: './chat.html',
  styleUrl: './chat.css'
})
export class ChatPage {
  private readonly chatService = inject(ChatService);

  protected readonly messages = signal<ChatMessage[]>([]);
  protected newMessage = '';
  protected readonly isLoading = signal(false);
  protected readonly isSending = signal(false);
  protected readonly errorMessage = signal('');

  constructor() {
    this.loadMessages();
  }

  protected loadMessages(): void {
    this.isLoading.set(true);
    this.errorMessage.set('');

    this.chatService.getMessages().subscribe({
      next: (messages) => {
        this.messages.set(messages);
        this.isLoading.set(false);
      },
      error: () => {
        this.errorMessage.set('Unable to load chat messages.');
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

    const payload: SendMessageRequest = { text };
    this.chatService.sendMessage(payload).subscribe({
      next: (message) => {
        this.messages.update((messages) => [...messages, message]);
        this.newMessage = '';
        this.isSending.set(false);
      },
      error: () => {
        this.errorMessage.set('Unable to send message.');
        this.isSending.set(false);
      }
    });
  }
}

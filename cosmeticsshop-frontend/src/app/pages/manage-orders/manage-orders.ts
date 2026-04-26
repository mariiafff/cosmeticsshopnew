import { Component, OnInit, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';

import { AuthService } from '../../services/auth.service';
import { Order, OrderService } from '../../services/order.service';
import { Store, StoresService } from '../../services/stores.service';

@Component({
  selector: 'app-manage-orders-page',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink],
  templateUrl: './manage-orders.html',
  styleUrl: './manage-orders.css'
})
export class ManageOrdersPage implements OnInit {
  private readonly authService = inject(AuthService);
  private readonly orderService = inject(OrderService);
  private readonly storesService = inject(StoresService);

  protected readonly orders = signal<Order[]>([]);
  protected readonly stores = signal<Store[]>([]);
  protected readonly isLoading = signal(false);
  protected readonly errorMessage = signal('');
  protected readonly successMessage = signal('');
  protected readonly storesWarning = signal('');
  protected readonly isCorporate = this.authService.getRole() === 'CORPORATE';

  ngOnInit(): void {
    this.loadStores();
    this.loadOrders();
  }

  protected loadOrders(): void {
    this.isLoading.set(true);
    this.errorMessage.set('');
    this.successMessage.set('');

    this.orderService.getAllOrders().subscribe({
      next: (orders) => {
        this.orders.set(orders);
        this.isLoading.set(false);
      },
      error: (error) => {
        this.errorMessage.set(this.extractErrorMessage(error, 'Unable to load orders.'));
        this.isLoading.set(false);
      }
    });
  }

  protected updateOrder(order: Order): void {
    this.orderService.updateOrder(order.id, order).subscribe({
      next: () => {
        this.successMessage.set('Order status updated.');
        this.errorMessage.set('');
        this.loadOrders();
      },
      error: (error) => {
        this.errorMessage.set(this.extractErrorMessage(error, 'Failed to update order.'));
        this.successMessage.set('');
      }
    });
  }

  protected deleteOrder(id: number): void {
    this.orderService.deleteOrder(id).subscribe({
      next: () => {
        this.orders.update((orders) => orders.filter((order) => order.id !== id));
        this.successMessage.set('Order deleted successfully.');
        this.errorMessage.set('');
      },
      error: (error) => {
        this.errorMessage.set(this.extractErrorMessage(error, 'Unable to delete order.'));
        this.successMessage.set('');
      }
    });
  }

  protected get hasStore(): boolean {
    return this.stores().length > 0;
  }

  protected canDeleteOrders(): boolean {
    return this.authService.getRole() === 'ADMIN';
  }

  private loadStores(): void {
    this.storesWarning.set('');
    this.storesService.getStores().subscribe({
      next: (stores) => this.stores.set(stores),
      error: (error) => {
        this.storesWarning.set(this.extractErrorMessage(error, 'Unable to load stores.'));
      }
    });
  }

  private extractErrorMessage(error: unknown, fallback: string): string {
    if (error instanceof HttpErrorResponse) {
      const backendMessage =
        typeof error.error?.message === 'string'
          ? error.error.message
          : typeof error.error === 'string'
            ? error.error
            : '';
      if (backendMessage) {
        return backendMessage;
      }
      if (error.status === 403) {
        return 'Your role does not have access to this order management action.';
      }
      if (error.status === 401) {
        return 'Your session expired. Please log in again.';
      }
    }

    return fallback;
  }
}

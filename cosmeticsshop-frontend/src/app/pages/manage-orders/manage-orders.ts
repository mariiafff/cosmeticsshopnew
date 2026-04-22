import { Component, OnInit, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';

import { Order, OrderService } from '../../services/order.service';

@Component({
  selector: 'app-manage-orders-page',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink],
  templateUrl: './manage-orders.html',
  styleUrl: './manage-orders.css'
})
export class ManageOrdersPage implements OnInit {
  private readonly orderService = inject(OrderService);

  protected readonly orders = signal<Order[]>([]);
  protected readonly isLoading = signal(false);
  protected readonly errorMessage = signal('');
  protected readonly successMessage = signal('');

  ngOnInit(): void {
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
      error: () => {
        this.errorMessage.set('Unable to load orders.');
        this.isLoading.set(false);
      }
    });
  }

  protected updateOrder(order: Order): void {
    this.orderService.updateOrder(order.id, order).subscribe({
      next: () => {
        this.successMessage.set('Order status updated.');
        this.errorMessage.set('');
      },
      error: () => {
        this.errorMessage.set('Failed to update order.');
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
      error: () => {
        this.errorMessage.set('Unable to delete order.');
        this.successMessage.set('');
      }
    });
  }
}

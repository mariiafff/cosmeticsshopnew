import { Component, OnInit, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule, NgForm } from '@angular/forms';
import { RouterLink } from '@angular/router';

import { CreateOrderRequest, Order, OrderService } from '../../services/order.service';

@Component({
  selector: 'app-orders-page',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink],
  templateUrl: './orders.html',
  styleUrl: './orders.css'
})
export class OrdersPage implements OnInit {
  private readonly orderService = inject(OrderService);

  protected readonly orders = signal<Order[]>([]);
  protected readonly isLoading = signal(false);
  protected readonly isSubmitting = signal(false);
  protected readonly errorMessage = signal('');
  protected readonly successMessage = signal('');

  protected readonly orderRequest: CreateOrderRequest = {
    productId: 0,
    quantity: 1,
    paymentMethod: 'CARD'
  };

  ngOnInit(): void {
    this.loadOrders();
  }

  protected loadOrders(): void {
    this.isLoading.set(true);
    this.errorMessage.set('');

    this.orderService.getMyOrders().subscribe({
      next: (orders) => {
        this.orders.set(orders);
        this.isLoading.set(false);
      },
      error: () => {
        this.errorMessage.set('Unable to load your orders. Please check your authentication and backend connection.');
        this.isLoading.set(false);
      }
    });
  }

  protected createOrder(form: NgForm): void {
    this.errorMessage.set('');
    this.successMessage.set('');

    if (form.invalid || this.orderRequest.productId <= 0 || this.orderRequest.quantity <= 0) {
      this.errorMessage.set('Product ID and quantity are required. Quantity must be at least 1.');
      form.control.markAllAsTouched();
      return;
    }

    this.isSubmitting.set(true);

    this.orderService.createOrder(this.orderRequest).subscribe({
      next: (order) => {
        this.orders.update((orders) => [order, ...orders]);
        this.successMessage.set('Order created successfully.');
        this.orderRequest.productId = 0;
        this.orderRequest.quantity = 1;
        this.orderRequest.paymentMethod = 'CARD';
        this.isSubmitting.set(false);
      },
      error: () => {
        this.errorMessage.set('Order creation failed. Check your token and backend endpoint.');
        this.isSubmitting.set(false);
      }
    });
  }
}

import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { CommonModule, isPlatformBrowser } from '@angular/common';
import { PLATFORM_ID } from '@angular/core';
import { Router, RouterLink } from '@angular/router';

import { Order, OrderService } from '../../services/order.service';
import { AuthService } from '../../services/auth.service';

@Component({
  selector: 'app-orders-page',
  standalone: true,
  imports: [CommonModule, RouterLink],
  templateUrl: './orders.html',
  styleUrl: './orders.css'
})
export class OrdersPage implements OnInit {
  private readonly platformId = inject(PLATFORM_ID);
  private readonly router = inject(Router);
  private readonly orderService = inject(OrderService);
  private readonly authService = inject(AuthService);

  protected readonly orders = signal<Order[]>([]);
  protected readonly isLoading = signal(false);
  protected readonly errorMessage = signal('');
  protected readonly successMessage = signal('');
  protected readonly role = computed(() => this.authService.getRole() ?? 'Guest');
  protected readonly roleSummary = computed(() => {
    const role = this.role().toUpperCase();
    if (role.includes('ADMIN')) {
      return 'Review store orders, delivery status, payment methods, and marketplace activity.';
    }
    if (role.includes('CORPORATE')) {
      return 'Follow store orders from checkout to delivery and handle customer requests quickly.';
    }
    return 'Check your purchases, track deliveries, and review your order history.';
  });
  protected readonly fulfillmentStages = ['Placed', 'Paid', 'Packed', 'Shipped', 'Delivered'];

  ngOnInit(): void {
    if (!isPlatformBrowser(this.platformId)) {
      return;
    }

    if (!this.authService.isLoggedIn()) {
      void this.router.navigate(['/login']);
      return;
    }

    this.loadOrders();
  }

  protected loadOrders(): void {
    this.isLoading.set(true);
    this.errorMessage.set('');

    this.orderService.getMyOrders().subscribe({
      next: (orders) => {
        this.orders.set(this.sortNewestFirst(orders));
        this.isLoading.set(false);
      },
      error: () => {
        this.errorMessage.set('Unable to load your orders. Please check your authentication and backend connection.');
        this.isLoading.set(false);
      }
    });
  }

  private sortNewestFirst(orders: Order[]): Order[] {
    return [...orders].sort((first, second) => {
      const firstTime = first.createdAt ? new Date(first.createdAt).getTime() : 0;
      const secondTime = second.createdAt ? new Date(second.createdAt).getTime() : 0;
      return secondTime - firstTime;
    });
  }
}

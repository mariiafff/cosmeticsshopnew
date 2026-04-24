import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { CommonModule, isPlatformBrowser } from '@angular/common';
import { PLATFORM_ID } from '@angular/core';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { forkJoin } from 'rxjs';

import { Order, OrderService } from '../../services/order.service';
import { AuthService } from '../../services/auth.service';
import { CartService } from '../../services/cart.service';

@Component({
  selector: 'app-orders-page',
  standalone: true,
  imports: [CommonModule, RouterLink],
  templateUrl: './orders.html',
  styleUrl: './orders.css'
})
export class OrdersPage implements OnInit {
  private readonly platformId = inject(PLATFORM_ID);
  private readonly route = inject(ActivatedRoute);
  private readonly orderService = inject(OrderService);
  private readonly authService = inject(AuthService);
  private readonly cartService = inject(CartService);

  protected readonly orders = signal<Order[]>([]);
  protected readonly isLoading = signal(false);
  protected readonly errorMessage = signal('');
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

    if (this.route.snapshot.queryParamMap.get('payment') === 'success') {
      this.createOrdersFromPaidCart();
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

  private createOrdersFromPaidCart(): void {
    const cartItems = this.cartService.items();

    if (cartItems.length === 0) {
      this.loadOrders();
      return;
    }

    this.isLoading.set(true);
    this.errorMessage.set('');

    forkJoin(
      cartItems.map((item) =>
        this.orderService.createOrder({
          productId: item.productId,
          quantity: item.quantity,
          paymentMethod: 'CARD'
        })
      )
    ).subscribe({
      next: () => {
        this.cartService.clear();
        this.loadOrders();
      },
      error: () => {
        this.errorMessage.set('Payment finished, but we could not save the order yet. Please refresh or try again.');
        this.isLoading.set(false);
      }
    });
  }
}

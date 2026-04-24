import { CommonModule } from '@angular/common';
import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { FormsModule, NgForm } from '@angular/forms';
import { ActivatedRoute, RouterLink } from '@angular/router';

import { CreateOrderRequest } from '../../services/order.service';
import { CartService } from '../../services/cart.service';
import { PaymentService } from '../../services/payment.service';

@Component({
  selector: 'app-checkout-page',
  imports: [CommonModule, FormsModule, RouterLink],
  templateUrl: './checkout.html',
  styleUrl: './checkout.css'
})
export class CheckoutPage implements OnInit {
  private readonly route = inject(ActivatedRoute);
  private readonly cartService = inject(CartService);
  private readonly paymentService = inject(PaymentService);

  protected readonly isSubmitting = signal(false);
  protected readonly errorMessage = signal('');
  protected readonly successMessage = signal('');
  protected readonly cartItems = this.cartService.items;
  protected readonly subtotal = this.cartService.subtotal;
  protected readonly hasCartItems = computed(() => this.cartItems().length > 0);

  protected readonly orderRequest: CreateOrderRequest = {
    productId: 0,
    quantity: 1,
    paymentMethod: 'CARD'
  };

  ngOnInit(): void {
    const productId = Number(this.route.snapshot.queryParamMap.get('productId') ?? 0);
    if (productId > 0) {
      this.orderRequest.productId = productId;
    }

    const firstItem = this.cartItems()[0];
    if (firstItem) {
      this.orderRequest.productId = firstItem.productId;
      this.orderRequest.quantity = firstItem.quantity;
    }
  }

  protected updateQuantity(productId: number, quantity: number): void {
    this.cartService.updateQuantity(productId, Number(quantity));
    const firstItem = this.cartItems()[0];
    this.orderRequest.productId = firstItem?.productId ?? 0;
    this.orderRequest.quantity = firstItem?.quantity ?? 1;
  }

  protected placeOrder(form: NgForm): void {
    this.errorMessage.set('');
    this.successMessage.set('');

    const firstItem = this.cartItems()[0];
    if (firstItem) {
      this.orderRequest.productId = firstItem.productId;
      this.orderRequest.quantity = firstItem.quantity;
    }

    if (!this.hasCartItems() || form.invalid || this.orderRequest.productId <= 0 || this.orderRequest.quantity <= 0) {
      this.errorMessage.set('Your cart is empty.');
      form.control.markAllAsTouched();
      return;
    }

    this.isSubmitting.set(true);

    this.paymentService.createCheckoutSession(this.cartItems()).subscribe({
      next: (session) => {
        window.location.href = session.url;
      },
      error: () => {
        this.errorMessage.set('We could not open payment right now.');
        this.isSubmitting.set(false);
      }
    });
  }
}

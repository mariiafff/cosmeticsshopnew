import { CommonModule } from '@angular/common';
import { Component, OnDestroy, OnInit, computed, effect, inject, signal } from '@angular/core';
import { FormsModule, NgForm } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { Subscription } from 'rxjs';

import { AuthService } from '../../services/auth.service';
import { CartService } from '../../services/cart.service';
import { CheckoutRequest, OrderService } from '../../services/order.service';
import { PaymentService } from '../../services/payment.service';

@Component({
  selector: 'app-checkout-page',
  imports: [CommonModule, FormsModule, RouterLink],
  templateUrl: './checkout.html',
  styleUrl: './checkout.css'
})
export class CheckoutPage implements OnInit, OnDestroy {
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly authService = inject(AuthService);
  private readonly cartService = inject(CartService);
  private readonly orderService = inject(OrderService);
  private readonly paymentService = inject(PaymentService);
  private toastTimeoutId: ReturnType<typeof setTimeout> | null = null;
  private redirectTimeoutId: ReturnType<typeof setTimeout> | null = null;
  private prefetchTimeoutId: ReturnType<typeof setTimeout> | null = null;
  private prefetchSubscription: Subscription | null = null;
  private latestPrefetchSignature = '';
  private checkoutSessionRetryTimeoutId: ReturnType<typeof setTimeout> | null = null;

  protected readonly isSubmitting = signal(false);
  protected readonly errorMessage = signal('');
  protected readonly successMessage = signal('');
  protected readonly toastMessage = signal('');
  protected readonly showSuccessOverlay = signal(false);
  protected readonly paymentCompleted = signal(false);
  protected readonly isPreparingStripe = signal(false);
  protected readonly cartItems = this.cartService.items;
  protected readonly subtotal = this.cartService.subtotal;
  protected readonly hasCartItems = computed(() => this.cartItems().length > 0);
  protected readonly prefetchedCheckoutUrl = signal<string | null>(null);
  protected paymentMethod = 'CARD';

  constructor() {
    effect(() => {
      const paymentState = this.route.snapshot.queryParamMap.get('payment');
      const currentPath = this.route.snapshot.routeConfig?.path ?? '';
      const signature = this.buildCartSignature(this.cartItems());

      if (paymentState === 'success' || currentPath === 'checkout/payment-success' || !signature) {
        this.clearPrefetchedCheckout();
        return;
      }

      this.scheduleCheckoutPrefetch(signature);
    });
  }

  ngOnInit(): void {
    const currentPath = this.route.snapshot.routeConfig?.path ?? '';
    if (this.route.snapshot.queryParamMap.get('payment') === 'success' || currentPath === 'checkout/payment-success') {
      this.isSubmitting.set(true);
      this.errorMessage.set('');
      this.successMessage.set('');
      this.paymentCompleted.set(true);
      this.showSuccessOverlay.set(false);
      this.authService.restoreCheckoutTokens();
      this.finalizePaidCheckout(8);
      return;
    }

    if (this.route.snapshot.queryParamMap.get('payment') === 'cancelled') {
      this.errorMessage.set('Payment was cancelled. Your cart is still here.');
    }
  }

  ngOnDestroy(): void {
    if (this.toastTimeoutId) {
      clearTimeout(this.toastTimeoutId);
      this.toastTimeoutId = null;
    }

    if (this.redirectTimeoutId) {
      clearTimeout(this.redirectTimeoutId);
      this.redirectTimeoutId = null;
    }

    if (this.prefetchTimeoutId) {
      clearTimeout(this.prefetchTimeoutId);
      this.prefetchTimeoutId = null;
    }

    if (this.checkoutSessionRetryTimeoutId) {
      clearTimeout(this.checkoutSessionRetryTimeoutId);
      this.checkoutSessionRetryTimeoutId = null;
    }

    this.prefetchSubscription?.unsubscribe();
    this.prefetchSubscription = null;
  }

  protected updateQuantity(productId: number, quantity: number): void {
    this.cartService.updateQuantity(productId, Number(quantity));
  }

  protected increaseQuantity(productId: number, currentQuantity: number): void {
    this.cartService.updateQuantity(productId, currentQuantity + 1);
  }

  protected decreaseQuantity(productId: number, currentQuantity: number): void {
    this.cartService.updateQuantity(productId, Math.max(1, currentQuantity - 1));
  }

  protected removeItem(productId: number, productName: string): void {
    this.cartService.removeItem(productId);
    this.showToast(`${productName} removed from cart.`);
    if (!this.hasCartItems()) {
      this.paymentService.clearPendingCheckout();
      this.isSubmitting.set(false);
    }
  }

  protected placeOrder(form: NgForm): void {
    this.errorMessage.set('');
    this.successMessage.set('');

    if (!this.hasCartItems() || form.invalid) {
      this.errorMessage.set('Your cart is empty.');
      form.control.markAllAsTouched();
      return;
    }

    this.isSubmitting.set(true);
    this.paymentService.rememberPendingCheckout(this.cartItems(), this.paymentMethod);

    const prefetchedUrl = this.prefetchedCheckoutUrl();
    const currentSignature = this.buildCartSignature(this.cartItems());
    if (prefetchedUrl && currentSignature === this.latestPrefetchSignature) {
      window.location.href = prefetchedUrl;
      return;
    }

    this.paymentService.createCheckoutSession(this.cartItems()).subscribe({
      next: (session) => {
        window.location.href = session.url;
      },
      error: () => {
        this.paymentService.clearPendingCheckout();
        this.errorMessage.set('We could not open payment right now.');
        this.isSubmitting.set(false);
      }
    });
  }

  private scheduleCheckoutPrefetch(signature: string): void {
    if (signature === this.latestPrefetchSignature && this.prefetchedCheckoutUrl()) {
      return;
    }

    if (this.prefetchTimeoutId) {
      clearTimeout(this.prefetchTimeoutId);
    }

    this.prefetchedCheckoutUrl.set(null);
    this.isPreparingStripe.set(true);

    this.prefetchTimeoutId = setTimeout(() => {
      this.prefetchSubscription?.unsubscribe();
      this.prefetchSubscription = this.paymentService.createCheckoutSession(this.cartItems()).subscribe({
        next: (session) => {
          const currentSignature = this.buildCartSignature(this.cartItems());
          if (currentSignature === signature) {
            this.latestPrefetchSignature = signature;
            this.prefetchedCheckoutUrl.set(session.url);
          }
          this.isPreparingStripe.set(false);
        },
        error: () => {
          this.prefetchedCheckoutUrl.set(null);
          this.isPreparingStripe.set(false);
        }
      });
      this.prefetchTimeoutId = null;
    }, 250);
  }

  private clearPrefetchedCheckout(): void {
    if (this.prefetchTimeoutId) {
      clearTimeout(this.prefetchTimeoutId);
      this.prefetchTimeoutId = null;
    }
    this.prefetchSubscription?.unsubscribe();
    this.prefetchSubscription = null;
    this.latestPrefetchSignature = '';
    this.prefetchedCheckoutUrl.set(null);
    this.isPreparingStripe.set(false);
  }

  private buildCartSignature(items: typeof this.cartItems extends () => infer T ? T : never): string {
    return items
      .map((item) => `${item.productId}:${item.quantity}`)
      .sort()
      .join('|');
  }

  private finalizePaidCheckout(retriesRemaining = 0): void {
    const request = this.paymentService.getPendingCheckoutRequest() ?? this.buildCheckoutRequestFromCart();

    if (!request || request.items.length === 0) {
      if (retriesRemaining > 0) {
        this.checkoutSessionRetryTimeoutId = setTimeout(() => {
          this.finalizePaidCheckout(retriesRemaining - 1);
        }, 250);
        return;
      }

      this.isSubmitting.set(false);
      this.paymentCompleted.set(false);
      this.showSuccessOverlay.set(false);
      this.errorMessage.set('We could not verify your payment session. Please review your cart and try again.');
      return;
    }

    this.isSubmitting.set(true);
    this.errorMessage.set('');
    this.successMessage.set('');

    this.orderService.checkout(request).subscribe({
      next: () => {
        this.paymentService.clearPendingCheckout();
        this.cartService.clear();
        this.isSubmitting.set(false);
        this.errorMessage.set('');
        this.paymentCompleted.set(true);
        this.showSuccessOverlay.set(true);
        this.redirectTimeoutId = setTimeout(() => {
          this.paymentService.clearCheckoutAuthBackup();
          void this.router.navigate(['/orders'], { replaceUrl: true });
        }, 2000);
      },
      error: (error) => {
        console.error('Order checkout failed', error);
        this.isSubmitting.set(false);
        this.paymentCompleted.set(false);
        this.showSuccessOverlay.set(false);
        this.errorMessage.set('Order could not be created after payment. Your cart was kept so you can try again.');
      }
    });
  }

  private buildCheckoutRequestFromCart(): CheckoutRequest | null {
    const items = this.cartService.items().map((item) => ({
      productId: item.productId,
      quantity: item.quantity
    }));

    if (!items.length) {
      return null;
    }

    return {
      items,
      paymentMethod: this.paymentMethod
    };
  }

  private showToast(message: string): void {
    this.toastMessage.set(message);
    if (this.toastTimeoutId) {
      clearTimeout(this.toastTimeoutId);
    }
    this.toastTimeoutId = setTimeout(() => {
      this.toastMessage.set('');
      this.toastTimeoutId = null;
    }, 2500);
  }
}

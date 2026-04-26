import { Injectable, computed, signal } from '@angular/core';

import { Product } from './products.service';

export interface CartItem {
  productId: number;
  name: string;
  price: number;
  quantity: number;
}

@Injectable({
  providedIn: 'root'
})
export class CartService {
  private readonly storageKey = 'luime_cart';
  private readonly itemsSignal = signal<CartItem[]>(this.loadCart());

  readonly items = this.itemsSignal.asReadonly();
  readonly itemCount = computed(() =>
    this.itemsSignal().reduce((total, item) => total + item.quantity, 0)
  );
  readonly subtotal = computed(() =>
    this.itemsSignal().reduce((total, item) => total + item.price * item.quantity, 0)
  );

  addProduct(product: Product): void {
    this.itemsSignal.update((items) => {
      const existing = items.find((item) => item.productId === product.id);
      const next = existing
        ? items.map((item) =>
            item.productId === product.id ? { ...item, quantity: item.quantity + 1 } : item
          )
        : [
            ...items,
            {
              productId: product.id,
              name: product.name,
              price: product.price ?? 0,
              quantity: 1
            }
          ];
      this.saveCart(next);
      return next;
    });
  }

  updateQuantity(productId: number, quantity: number): void {
    const safeQuantity = Number.isFinite(quantity) && quantity >= 1 ? Math.floor(quantity) : 1;
    const next = this.itemsSignal()
      .map((item) => (item.productId === productId ? { ...item, quantity: safeQuantity } : item))
      .filter((item) => item.quantity > 0);
    this.itemsSignal.set(next);
    this.saveCart(next);
  }

  removeItem(productId: number): void {
    const next = this.itemsSignal().filter((item) => item.productId !== productId);
    this.itemsSignal.set(next);
    this.saveCart(next);
  }

  clear(): void {
    this.itemsSignal.set([]);
    this.saveCart([]);
  }

  private loadCart(): CartItem[] {
    if (typeof localStorage === 'undefined') {
      return [];
    }

    try {
      const raw = localStorage.getItem(this.storageKey);
      return raw ? (JSON.parse(raw) as CartItem[]) : [];
    } catch {
      return [];
    }
  }

  private saveCart(items: CartItem[]): void {
    if (typeof localStorage === 'undefined') {
      return;
    }
    localStorage.setItem(this.storageKey, JSON.stringify(items));
  }
}

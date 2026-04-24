import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';

import { Product, ProductsService } from '../../services/products.service';
import { AuthService } from '../../services/auth.service';
import { CartService } from '../../services/cart.service';

@Component({
  selector: 'app-products-page',
  imports: [CommonModule, FormsModule, RouterLink],
  templateUrl: './products.html',
  styleUrl: './products.css'
})
export class ProductsPage implements OnInit {
  private readonly productsService = inject(ProductsService);
  private readonly authService = inject(AuthService);
  private readonly cartService = inject(CartService);

  protected readonly products = signal<Product[]>([]);
  protected readonly isLoading = signal(false);
  protected readonly errorMessage = signal('');
  protected readonly successMessage = signal('');
  protected searchQuery = '';
  protected categoryFilter = 'ALL';
  protected stockFilter = 'ALL';
  protected sortMode = 'name';
  protected readonly role = signal(this.authService.getRole() ?? 'GUEST');
  protected readonly categories = computed(() => {
    const values = this.products()
      .map((product) => product.category?.name)
      .filter((name): name is string => !!name);
    return Array.from(new Set(values)).sort();
  });
  protected readonly filteredProducts = computed(() => {
    const products = this.products().filter((product) => {
      const matchesCategory =
        this.categoryFilter === 'ALL' || product.category?.name === this.categoryFilter;
      const stock = product.stockQuantity ?? 0;
      const matchesStock =
        this.stockFilter === 'ALL' ||
        (this.stockFilter === 'LOW' && stock <= 5) ||
        (this.stockFilter === 'AVAILABLE' && stock > 5);
      return matchesCategory && matchesStock;
    });

    return products.sort((first, second) => {
      if (this.sortMode === 'price') {
        return (first.price ?? 0) - (second.price ?? 0);
      }
      if (this.sortMode === 'stock') {
        return (second.stockQuantity ?? 0) - (first.stockQuantity ?? 0);
      }
      return first.name.localeCompare(second.name);
    });
  });
  protected readonly canManageProducts = computed(() => {
    const role = this.role().toUpperCase();
    return role.includes('ADMIN') || role.includes('CORPORATE');
  });

  ngOnInit(): void {
    this.loadProducts();
  }

  protected loadProducts(): void {
    this.isLoading.set(true);
    this.errorMessage.set('');

    this.productsService.getProducts(this.searchQuery).subscribe({
      next: (products) => {
        this.products.set(products);
        this.isLoading.set(false);
      },
      error: () => {
        this.errorMessage.set('We could not load products right now.');
        this.isLoading.set(false);
      }
    });
  }

  protected addToCart(product: Product): void {
    this.cartService.addProduct(product);
    this.successMessage.set(`${product.name} added to cart.`);
  }
}

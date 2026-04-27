import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';

import { Product, ProductsPage as ProductsPageResponse, ProductsService } from '../../services/products.service';
import { AuthService } from '../../services/auth.service';
import { CartService } from '../../services/cart.service';

@Component({
  selector: 'app-products-page',
  imports: [CommonModule, FormsModule, RouterLink],
  templateUrl: './products.html',
  styleUrl: './products.css'
})
export class ProductsPage implements OnInit {
  private static readonly PAGE_SIZE = 24;
  private static readonly TOAST_DURATION_MS = 2500;
  protected readonly fallbackImage = 'https://via.placeholder.com/300';

  private readonly productsService = inject(ProductsService);
  private readonly authService = inject(AuthService);
  private readonly cartService = inject(CartService);
  private toastTimeoutId: ReturnType<typeof setTimeout> | null = null;

  protected readonly products = signal<Product[]>([]);
  protected readonly isLoading = signal(false);
  protected readonly isLoadingMore = signal(false);
  protected readonly errorMessage = signal('');
  protected readonly toastMessage = signal('');
  protected readonly currentPage = signal(0);
  protected readonly totalPages = signal(0);
  protected readonly totalElements = signal(0);
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
  protected readonly canLoadMore = computed(() => {
    return !this.isLoading() && !this.isLoadingMore() && this.currentPage() + 1 < this.totalPages();
  });
  protected readonly canBuy = computed(() => {
    return this.role().toUpperCase() === 'INDIVIDUAL';
  });
  protected readonly loadedCount = computed(() => this.products().length);

  ngOnInit(): void {
    this.loadProducts();
  }

  protected loadProducts(loadMore = false): void {
    const nextPage = loadMore ? this.currentPage() + 1 : 0;

    if (loadMore) {
      this.isLoadingMore.set(true);
    } else {
      this.isLoading.set(true);
    }
    this.errorMessage.set('');

    this.productsService.getProductsPage({
      page: nextPage,
      size: ProductsPage.PAGE_SIZE,
      search: this.searchQuery,
      sort: this.resolveSort()
    }).subscribe({
      next: (response) => {
        console.log('Products API response:', response);
        console.log('First product imageUrl:', response.content?.[0]?.imageUrl);
        this.applyPage(response, loadMore);
        this.isLoading.set(false);
        this.isLoadingMore.set(false);
      },
      error: () => {
        this.errorMessage.set('We could not load products right now.');
        this.isLoading.set(false);
        this.isLoadingMore.set(false);
      }
    });
  }

  protected applySort(): void {
    this.loadProducts();
  }

  protected loadMore(): void {
    if (!this.canLoadMore()) {
      return;
    }
    this.loadProducts(true);
  }

  protected productStockLabel(product: Product): string {
    const stock = product.stockQuantity ?? 0;
    return stock > 0 ? `Stock: ${stock}` : 'Stock: N/A';
  }

  protected getProductImage(product: Product): string {
    return product.imageUrl || this.fallbackImage;
  }

  protected onImageError(event: Event): void {
    const image = event.target as HTMLImageElement | null;
    if (!image) {
      return;
    }
    image.src = this.fallbackImage;
  }

  protected trackByProductId(_index: number, product: Product): number {
    return product.id;
  }

  protected addToCart(product: Product): void {
    this.cartService.addProduct(product);
    this.showToast(`${product.name} added to cart.`);
  }

  private applyPage(response: ProductsPageResponse, append: boolean): void {
    const mergedProducts = append
      ? [...this.products(), ...response.content]
      : response.content;

    this.products.set(mergedProducts);
    this.currentPage.set(response.number);
    this.totalPages.set(response.totalPages);
    this.totalElements.set(response.totalElements);
  }

  private resolveSort(): string {
    if (this.sortMode === 'price') {
      return 'price,asc';
    }
    if (this.sortMode === 'stock') {
      return 'stock,desc';
    }
    return 'name,asc';
  }

  private showToast(message: string): void {
    this.toastMessage.set(message);

    if (this.toastTimeoutId) {
      clearTimeout(this.toastTimeoutId);
    }

    this.toastTimeoutId = setTimeout(() => {
      this.toastMessage.set('');
      this.toastTimeoutId = null;
    }, ProductsPage.TOAST_DURATION_MS);
  }
}

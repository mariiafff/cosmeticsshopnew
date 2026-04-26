import { Component, OnInit, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { FormsModule, NgForm } from '@angular/forms';
import { RouterLink } from '@angular/router';

import { AuthService } from '../../services/auth.service';
import { Category, CategoriesService } from '../../services/categories.service';
import { Product, ProductsService } from '../../services/products.service';
import { Store, StoresService } from '../../services/stores.service';

interface ProductForm {
  name: string;
  description?: string;
  price: number;
  sku?: string;
  stockQuantity: number;
  status: string;
  storeId?: number;
  categoryId?: number;
}

@Component({
  selector: 'app-manage-products-page',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink],
  templateUrl: './manage-products.html',
  styleUrl: './manage-products.css'
})
export class ManageProductsPage implements OnInit {
  private readonly authService = inject(AuthService);
  private readonly productsService = inject(ProductsService);
  private readonly storesService = inject(StoresService);
  private readonly categoriesService = inject(CategoriesService);

  protected readonly products = signal<Product[]>([]);
  protected readonly stores = signal<Store[]>([]);
  protected readonly categories = signal<Category[]>([]);
  protected readonly isLoading = signal(false);
  protected readonly isSubmitting = signal(false);
  protected readonly errorMessage = signal('');
  protected readonly successMessage = signal('');
  protected readonly storesWarning = signal('');
  protected readonly categoriesWarning = signal('');
  protected readonly isCreatingStore = signal(false);
  protected readonly storeSuccessMessage = signal('');
  protected readonly storeErrorMessage = signal('');
  protected readonly showCreateStoreForm = signal(false);
  protected editingProductId: number | null = null;
  protected readonly isCorporate = this.authService.getRole() === 'CORPORATE';

  protected readonly productForm: ProductForm = {
    name: '',
    description: '',
    price: 0,
    sku: '',
    stockQuantity: 0,
    status: 'ACTIVE',
    storeId: undefined,
    categoryId: undefined
  };

  protected newStoreName = '';

  ngOnInit(): void {
    this.loadPageData();
  }

  protected loadPageData(): void {
    this.isLoading.set(true);
    this.errorMessage.set('');
    this.storesWarning.set('');
    this.categoriesWarning.set('');

    this.productsService.getManageProducts().subscribe({
      next: (products) => {
        this.products.set(products);
        this.isLoading.set(false);
      },
      error: (error) => {
        this.errorMessage.set(this.extractErrorMessage(error, 'Could not load products.'));
        this.isLoading.set(false);
      }
    });

    this.storesService.getStores().subscribe({
      next: (stores) => {
        this.stores.set(stores);
        this.syncDefaultStoreSelection(stores);
      },
      error: (error) => {
        this.storesWarning.set(this.extractErrorMessage(error, 'Could not load stores.'));
      }
    });

    this.categoriesService.getCategories().subscribe({
      next: (categories) => this.categories.set(categories),
      error: (error) => {
        this.categoriesWarning.set(this.extractErrorMessage(error, 'Could not load categories.'));
      }
    });
  }

  protected refresh(): void {
    this.loadPageData();
  }

  protected get hasStore(): boolean {
    return this.stores().length > 0;
  }

  protected toggleCreateStoreForm(): void {
    this.showCreateStoreForm.update((value) => !value);
    this.storeErrorMessage.set('');
    this.storeSuccessMessage.set('');
  }

  protected editProduct(product: Product): void {
    this.editingProductId = product.id;
    this.productForm.name = product.name;
    this.productForm.description = product.description || '';
    this.productForm.price = product.price ?? 0;
    this.productForm.sku = product.sku || '';
    this.productForm.stockQuantity = product.stockQuantity ?? 0;
    this.productForm.status = product.status || 'ACTIVE';
    this.productForm.storeId = product.store?.id;
    this.productForm.categoryId = product.category?.id;
    this.successMessage.set('');
    this.errorMessage.set('');
  }

  protected cancelEdit(): void {
    this.editingProductId = null;
    this.resetForm();
  }

  protected submitProduct(form: NgForm): void {
    this.errorMessage.set('');
    this.successMessage.set('');

    if (form.invalid || !this.productForm.name || this.productForm.price < 0) {
      this.errorMessage.set('Please provide a product name and a valid price.');
      form.control.markAllAsTouched();
      return;
    }

    this.isSubmitting.set(true);
    const payload = {
      name: this.productForm.name,
      description: this.productForm.description,
      price: this.productForm.price,
      sku: this.productForm.sku,
      stockQuantity: this.productForm.stockQuantity,
      status: this.productForm.status,
      store: this.productForm.storeId ? { id: this.productForm.storeId } : undefined,
      category: this.productForm.categoryId ? { id: this.productForm.categoryId } : undefined
    };

    const isEditMode = this.editingProductId !== null;
    const editingProductId = this.editingProductId;
    const action = isEditMode
      ? this.productsService.updateProduct(editingProductId as number, payload)
      : this.productsService.createProduct(payload);

    action.subscribe({
      next: () => {
        this.successMessage.set(isEditMode ? 'Product updated successfully.' : 'Product created successfully.');
        this.isSubmitting.set(false);
        this.cancelEdit();
        this.loadProductsOnly();
      },
      error: (error) => {
        this.errorMessage.set(
          this.extractErrorMessage(error, isEditMode ? 'Could not update product.' : 'Could not create product.')
        );
        this.isSubmitting.set(false);
      }
    });
  }

  protected createStore(form: NgForm): void {
    this.storeErrorMessage.set('');
    this.storeSuccessMessage.set('');

    if (!this.newStoreName.trim()) {
      this.storeErrorMessage.set('Please enter a store name.');
      form.control.markAllAsTouched();
      return;
    }

    this.isCreatingStore.set(true);
    this.storesService.createStore({ name: this.newStoreName.trim(), status: 'OPEN' }).subscribe({
      next: (store) => {
        this.stores.update((stores) => [...stores, store]);
        this.newStoreName = '';
        this.productForm.storeId = store.id;
        this.showCreateStoreForm.set(false);
        this.storeSuccessMessage.set('Store created successfully. You can now manage products for it.');
        this.storeErrorMessage.set('');
        this.isCreatingStore.set(false);
        form.resetForm();
      },
      error: (error) => {
        this.storeErrorMessage.set(this.extractErrorMessage(error, 'Could not create store.'));
        this.storeSuccessMessage.set('');
        this.isCreatingStore.set(false);
      }
    });
  }

  protected deleteProduct(product: Product): void {
    this.errorMessage.set('');
    this.successMessage.set('');

    if (product.id === undefined || product.id === null) {
      this.errorMessage.set('Could not deactivate product because the product ID is missing.');
      return;
    }

    this.productsService.deleteProduct(product.id).subscribe({
      next: () => {
        this.successMessage.set('Product deactivated successfully.');
        this.loadProductsOnly();
      },
      error: (error) => this.errorMessage.set(this.extractErrorMessage(error, 'Could not deactivate product.'))
    });
  }

  protected activateProduct(product: Product): void {
    this.errorMessage.set('');
    this.successMessage.set('');

    if (product.id === undefined || product.id === null) {
      this.errorMessage.set('Could not activate product because the product ID is missing.');
      return;
    }

    this.productsService.activateProduct(product.id).subscribe({
      next: () => {
        this.successMessage.set('Product activated successfully.');
        this.loadProductsOnly();
      },
      error: (error) => this.errorMessage.set(this.extractErrorMessage(error, 'Could not activate product.'))
    });
  }

  protected displayStoreName(product: Product): string {
    return product.store?.name || 'Not assigned';
  }

  protected displayCategoryName(product: Product): string {
    return product.category?.name || 'Uncategorized';
  }

  protected displayStatus(product: Product): string {
    return product.status || 'ACTIVE';
  }

  protected canDeactivate(product: Product): boolean {
    return this.displayStatus(product) !== 'INACTIVE';
  }

  protected canActivate(product: Product): boolean {
    return this.displayStatus(product) === 'INACTIVE';
  }

  protected shouldShowStoreSelector(): boolean {
    return !this.isCorporate || this.stores().length > 1;
  }

  protected currentStoreName(): string {
    const storeId = this.productForm.storeId;
    if (!storeId) {
      return this.stores()[0]?.name ?? 'No store selected';
    }
    return this.stores().find((store) => store.id === storeId)?.name ?? 'No store selected';
  }

  private resetForm(): void {
    this.productForm.name = '';
    this.productForm.description = '';
    this.productForm.price = 0;
    this.productForm.sku = '';
    this.productForm.stockQuantity = 0;
    this.productForm.status = 'ACTIVE';
    this.productForm.storeId = undefined;
    this.productForm.categoryId = undefined;
  }

  private loadProductsOnly(): void {
    this.isLoading.set(true);
    this.productsService.getManageProducts().subscribe({
      next: (products) => {
        this.products.set(products);
        this.isLoading.set(false);
      },
      error: (error) => {
        this.errorMessage.set(this.extractErrorMessage(error, 'Could not load products.'));
        this.isLoading.set(false);
      }
    });
  }

  private syncDefaultStoreSelection(stores: Store[]): void {
    if (stores.length >= 1 && this.isCorporate && !this.productForm.storeId) {
      this.productForm.storeId = stores[0].id;
      return;
    }

    if (stores.length === 1) {
      this.productForm.storeId = stores[0].id;
      return;
    }

    if (this.productForm.storeId && stores.some((store) => store.id === this.productForm.storeId)) {
      return;
    }

    this.productForm.storeId = undefined;
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
        return 'You do not have permission to perform this action.';
      }
      if (error.status === 401) {
        return 'Your session expired. Please log in again.';
      }
    }

    return fallback;
  }
}

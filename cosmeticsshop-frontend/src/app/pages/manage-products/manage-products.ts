import { Component, OnInit, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule, NgForm } from '@angular/forms';
import { RouterLink } from '@angular/router';

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
  protected editingProductId: number | null = null;

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

  ngOnInit(): void {
    this.loadPageData();
  }

  protected loadPageData(): void {
    this.isLoading.set(true);
    this.errorMessage.set('');

    this.productsService.getProducts().subscribe({
      next: (products) => {
        this.products.set(products);
        this.isLoading.set(false);
      },
      error: () => {
        this.errorMessage.set('Could not load products.');
        this.isLoading.set(false);
      }
    });

    this.storesService.getStores().subscribe({
      next: (stores) => this.stores.set(stores),
      error: () => {
        this.errorMessage.set('Could not load stores.');
      }
    });

    this.categoriesService.getCategories().subscribe({
      next: (categories) => this.categories.set(categories),
      error: () => {
        this.errorMessage.set('Could not load categories.');
      }
    });
  }

  protected refresh(): void {
    this.loadPageData();
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

    const action = this.editingProductId
      ? this.productsService.updateProduct(this.editingProductId, payload)
      : this.productsService.createProduct(payload);

    action.subscribe({
      next: () => {
        this.successMessage.set(this.editingProductId ? 'Product updated successfully.' : 'Product created successfully.');
        this.isSubmitting.set(false);
        this.cancelEdit();
        this.productsService.getProducts().subscribe((products) => this.products.set(products));
      },
      error: () => {
        this.errorMessage.set('Unable to save the product.');
        this.isSubmitting.set(false);
      }
    });
  }

  protected deleteProduct(id: number): void {
    this.productsService.deleteProduct(id).subscribe({
      next: () => this.productsService.getProducts().subscribe((products) => this.products.set(products)),
      error: () => this.errorMessage.set('Could not delete product.')
    });
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
}

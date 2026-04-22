import { Component, OnInit, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule, NgForm } from '@angular/forms';
import { RouterLink } from '@angular/router';

import { Store, StoreRequest, StoresService } from '../../services/stores.service';

@Component({
  selector: 'app-admin-stores-page',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink],
  templateUrl: './admin-stores.html',
  styleUrl: './admin-stores.css'
})
export class AdminStoresPage implements OnInit {
  private readonly storesService = inject(StoresService);

  protected readonly stores = signal<Store[]>([]);
  protected readonly isLoading = signal(false);
  protected readonly isSubmitting = signal(false);
  protected readonly errorMessage = signal('');
  protected readonly successMessage = signal('');
  protected editingStoreId: number | null = null;

  protected readonly storeRequest: StoreRequest = {
    name: '',
    status: 'OPEN',
    city: '',
    country: '',
    description: ''
  };

  ngOnInit(): void {
    this.loadStores();
  }

  protected loadStores(): void {
    this.isLoading.set(true);
    this.errorMessage.set('');
    this.successMessage.set('');

    this.storesService.getStores().subscribe({
      next: (stores) => {
        this.stores.set(stores);
        this.isLoading.set(false);
      },
      error: () => {
        this.errorMessage.set('Could not load stores.');
        this.isLoading.set(false);
      }
    });
  }

  protected editStore(store: Store): void {
    this.editingStoreId = store.id;
    this.storeRequest.name = store.name;
    this.storeRequest.status = store.status || 'OPEN';
    this.storeRequest.city = store.city || '';
    this.storeRequest.country = store.country || '';
    this.storeRequest.description = store.description || '';
    this.successMessage.set('');
    this.errorMessage.set('');
  }

  protected cancelEdit(): void {
    this.editingStoreId = null;
    this.resetForm();
  }

  protected submitStore(form: NgForm): void {
    this.errorMessage.set('');
    this.successMessage.set('');

    if (form.invalid || !this.storeRequest.name) {
      this.errorMessage.set('Store name is required.');
      form.control.markAllAsTouched();
      return;
    }

    this.isSubmitting.set(true);

    const action = this.editingStoreId
      ? this.storesService.updateStore(this.editingStoreId, this.storeRequest)
      : this.storesService.createStore(this.storeRequest);

    action.subscribe({
      next: () => {
        this.successMessage.set(this.editingStoreId ? 'Store updated.' : 'Store created.');
        this.isSubmitting.set(false);
        this.cancelEdit();
        this.loadStores();
      },
      error: () => {
        this.errorMessage.set('Could not save store.');
        this.isSubmitting.set(false);
      }
    });
  }

  protected deleteStore(store: Store): void {
    this.storesService.deleteStore(store.id).subscribe({
      next: () => {
        this.successMessage.set('Store deleted.');
        this.stores.update((stores) => stores.filter((item) => item.id !== store.id));
      },
      error: () => {
        this.errorMessage.set('Unable to delete store.');
      }
    });
  }

  private resetForm(): void {
    this.storeRequest.name = '';
    this.storeRequest.status = 'OPEN';
    this.storeRequest.city = '';
    this.storeRequest.country = '';
    this.storeRequest.description = '';
  }
}

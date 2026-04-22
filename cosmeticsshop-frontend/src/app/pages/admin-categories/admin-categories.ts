import { Component, OnInit, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule, NgForm } from '@angular/forms';
import { RouterLink } from '@angular/router';

import { Category, CategoryRequest, CategoriesService } from '../../services/categories.service';

@Component({
  selector: 'app-admin-categories-page',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink],
  templateUrl: './admin-categories.html',
  styleUrl: './admin-categories.css'
})
export class AdminCategoriesPage implements OnInit {
  private readonly categoriesService = inject(CategoriesService);

  protected readonly categories = signal<Category[]>([]);
  protected readonly isLoading = signal(false);
  protected readonly isSubmitting = signal(false);
  protected readonly errorMessage = signal('');
  protected readonly successMessage = signal('');
  protected editingCategoryId: number | null = null;

  protected readonly categoryRequest: CategoryRequest = {
    name: '',
    description: '',
    parentCategoryId: undefined
  };

  ngOnInit(): void {
    this.loadCategories();
  }

  protected loadCategories(): void {
    this.isLoading.set(true);
    this.errorMessage.set('');
    this.successMessage.set('');

    this.categoriesService.getCategories().subscribe({
      next: (categories) => {
        this.categories.set(categories);
        this.isLoading.set(false);
      },
      error: () => {
        this.errorMessage.set('Could not load categories.');
        this.isLoading.set(false);
      }
    });
  }

  protected editCategory(category: Category): void {
    this.editingCategoryId = category.id;
    this.categoryRequest.name = category.name;
    this.categoryRequest.description = category.description || '';
    this.categoryRequest.parentCategoryId = category.parentCategoryId;
    this.errorMessage.set('');
    this.successMessage.set('');
  }

  protected cancelEdit(): void {
    this.editingCategoryId = null;
    this.resetForm();
  }

  protected submitCategory(form: NgForm): void {
    this.errorMessage.set('');
    this.successMessage.set('');

    if (form.invalid || !this.categoryRequest.name) {
      this.errorMessage.set('Category name is required.');
      form.control.markAllAsTouched();
      return;
    }

    this.isSubmitting.set(true);

    const action = this.editingCategoryId
      ? this.categoriesService.updateCategory(this.editingCategoryId, this.categoryRequest)
      : this.categoriesService.createCategory(this.categoryRequest);

    action.subscribe({
      next: () => {
        this.successMessage.set(this.editingCategoryId ? 'Category updated.' : 'Category created.');
        this.isSubmitting.set(false);
        this.cancelEdit();
        this.loadCategories();
      },
      error: () => {
        this.errorMessage.set('Could not save category.');
        this.isSubmitting.set(false);
      }
    });
  }

  protected deleteCategory(category: Category): void {
    this.categoriesService.deleteCategory(category.id).subscribe({
      next: () => {
        this.successMessage.set('Category deleted.');
        this.categories.update((list) => list.filter((item) => item.id !== category.id));
      },
      error: () => {
        this.errorMessage.set('Unable to delete category.');
      }
    });
  }

  private resetForm(): void {
    this.categoryRequest.name = '';
    this.categoryRequest.description = '';
    this.categoryRequest.parentCategoryId = undefined;
  }
}

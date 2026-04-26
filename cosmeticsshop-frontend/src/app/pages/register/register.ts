import { Component, inject, signal } from '@angular/core';
import { FormsModule, NgForm } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';

import { HttpErrorResponse } from '@angular/common/http';

import { AuthService, RegisterRequest } from '../../services/auth.service';

@Component({
  selector: 'app-register-page',
  imports: [FormsModule, RouterLink],
  templateUrl: './register.html',
  styleUrl: './register.css'
})
export class RegisterPage {
  private readonly authService = inject(AuthService);
  private readonly router = inject(Router);

  protected readonly isSubmitting = signal(false);
  protected readonly errorMessage = signal('');
  protected readonly registration: RegisterRequest = {
    firstName: '',
    lastName: '',
    email: '',
    password: '',
    accountType: 'SHOPPER',
    city: '',
    membershipType: 'GOLD'
  };

  protected register(form: NgForm): void {
    this.errorMessage.set('');

    if (form.invalid) {
      form.control.markAllAsTouched();
      this.errorMessage.set('Please complete the required registration fields.');
      return;
    }

    this.isSubmitting.set(true);
    this.authService.register(this.registration).subscribe({
      next: () => {
        this.isSubmitting.set(false);
        void this.router.navigate(['/login'], { queryParams: { registered: 'true' } });
      },
      error: (error: HttpErrorResponse) => {
        console.error('Registration failed:', error.error ?? error.message);
        if (error.status === 0) {
          this.errorMessage.set('We cannot create accounts right now. Please try again soon.');
        } else if (typeof error.error?.message === 'string' && error.error.message.trim()) {
          this.errorMessage.set(error.error.message);
        } else {
          this.errorMessage.set('Registration failed. Please check the form and try again.');
        }
        this.isSubmitting.set(false);
      }
    });
  }
}

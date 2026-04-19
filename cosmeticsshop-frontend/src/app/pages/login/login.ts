import { Component, inject, signal } from '@angular/core';
import { FormsModule, NgForm } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';

import { HttpErrorResponse } from '@angular/common/http';

import { AuthService, LoginRequest } from '../../services/auth.service';

@Component({
  selector: 'app-login-page',
  imports: [FormsModule, RouterLink],
  templateUrl: './login.html',
  styleUrl: './login.css'
})
export class LoginPage {
  private readonly authService = inject(AuthService);
  private readonly router = inject(Router);

  protected readonly errorMessage = signal('');
  protected readonly successMessage = signal('');
  protected readonly isSubmitting = signal(false);

  protected readonly credentials: LoginRequest = {
    email: '',
    password: ''
  };

  protected login(form: NgForm): void {
    this.errorMessage.set('');
    this.successMessage.set('');

    if (form.invalid) {
      this.errorMessage.set('Please provide a valid email and password.');
      form.control.markAllAsTouched();
      return;
    }

    this.isSubmitting.set(true);

    this.authService.login(this.credentials).subscribe({
      next: (response) => {
        this.isSubmitting.set(false);
        void this.router.navigate(['/dashboard']);
      },
      error: (error: HttpErrorResponse) => {
        if (error.status === 0) {
          this.errorMessage.set('Backend API is not reachable on http://localhost:8080. Start the Spring Boot backend first.');
        } else if (error.status === 401 || error.status === 403) {
          this.errorMessage.set('Email or password is incorrect.');
        } else {
          this.errorMessage.set('Login failed. Please try again.');
        }
        this.isSubmitting.set(false);
      }
    });
  }
}

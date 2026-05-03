import { Component, inject, signal, OnInit } from '@angular/core';
import { FormsModule, NgForm } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';

import { HttpErrorResponse } from '@angular/common/http';

import { AuthService, LoginRequest } from '../../services/auth.service';
import { environment } from '../../../environments/environment';

@Component({
  selector: 'app-login-page',
  imports: [FormsModule, RouterLink],
  templateUrl: './login.html',
  styleUrl: './login.css'
})
export class LoginPage implements OnInit {
  private readonly authService = inject(AuthService);
  private readonly router = inject(Router);
  private readonly route = inject(ActivatedRoute);

  protected readonly errorMessage = signal('');
  protected readonly successMessage = signal('');
  protected readonly isSubmitting = signal(false);
  protected readonly googleLoginUrl = `${environment.apiBaseUrl.replace(/\/api$/, '')}/oauth2/authorization/google`;

  protected readonly credentials: LoginRequest = {
    email: '',
    password: ''
  };

  ngOnInit(): void {
    if (this.route.snapshot.queryParams['registered'] === 'true') {
      this.successMessage.set('Registration successful. Please login.');
    }
    if (this.route.snapshot.queryParams['oauthError'] === 'true') {
      this.errorMessage.set('Google login could not be completed. Check the Google OAuth setup and try again.');
    }
  }

  protected login(form: NgForm): void {
    this.errorMessage.set('');
    this.successMessage.set('');

    this.credentials.email = this.credentials.email?.trim().toLowerCase() ?? '';
    this.credentials.password = this.credentials.password?.trim() ?? '';

    if (form.invalid || !this.credentials.email || !this.credentials.password) {
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
        console.error('Login failed:', error.error ?? error.message);
        if (error.status === 0) {
          this.errorMessage.set('Cannot reach the backend at http://localhost:8080. Make sure Spring Boot is running.');
        } else if (typeof error.error?.message === 'string' && error.error.message.trim()) {
          this.errorMessage.set(error.error.message);
        } else {
          this.errorMessage.set(`Login failed (${error.status}). Please try again.`);
        }
        this.isSubmitting.set(false);
      }
    });
  }
}

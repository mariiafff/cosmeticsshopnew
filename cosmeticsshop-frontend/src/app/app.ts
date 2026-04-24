import { CommonModule } from '@angular/common';
import { Component, computed, inject, signal } from '@angular/core';
import { Router, RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';

import { AuthService } from './services/auth.service';
import { CartService } from './services/cart.service';

@Component({
  selector: 'app-root',
  imports: [CommonModule, RouterOutlet, RouterLink, RouterLinkActive],
  templateUrl: './app.html',
  styleUrl: './app.css'
})
export class App {
  private readonly authService = inject(AuthService);
  private readonly cartService = inject(CartService);
  private readonly router = inject(Router);

  protected readonly title = signal('cosmeticsshop-frontend');
  protected readonly isLoggedIn = this.authService.isAuthenticated;
  protected readonly role = computed(() => this.authService.currentRole() ?? 'Guest');
  protected readonly email = computed(() => this.authService.currentEmail() ?? 'Not signed in');
  protected readonly displayName = computed(() => {
    const email = this.authService.currentEmail();
    if (!email) {
      return '';
    }
    const name = email.split('@')[0].split(/[._-]/)[0];
    return name ? `${name.charAt(0).toUpperCase()}${name.slice(1)}` : 'there';
  });
  protected readonly cartCount = this.cartService.itemCount;

  protected logout(): void {
    this.authService.logout();
    this.cartService.clear();
    this.router.navigate(['/login']);
  }
}

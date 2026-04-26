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
  protected readonly isLoggedIn = computed(() => this.authService.isLoggedIn());
  protected readonly role = computed(() => this.authService.getUser()?.role ?? 'Guest');
  protected readonly email = computed(() => this.authService.getUser()?.email ?? 'Not signed in');
  protected readonly displayName = computed(() => {
    const user = this.authService.getUser();
    if (!user) {
      return '';
    }
    return user.name || user.email;
  });
  protected readonly cartCount = this.cartService.itemCount;

  protected logout(): void {
    this.authService.logout();
    this.cartService.clear();
    this.router.navigate(['/login']);
  }
}

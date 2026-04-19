import { CommonModule } from '@angular/common';
import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';

import { AnalyticsService, DashboardOverview } from '../../services/analytics.service';
import { AuthService } from '../../services/auth.service';

@Component({
  selector: 'app-dashboard-page',
  imports: [CommonModule, RouterLink],
  templateUrl: './dashboard.html',
  styleUrl: './dashboard.css'
})
export class DashboardPage implements OnInit {
  private readonly analyticsService = inject(AnalyticsService);
  private readonly authService = inject(AuthService);

  protected readonly overview = signal<DashboardOverview | null>(null);
  protected readonly isLoading = signal(false);
  protected readonly errorMessage = signal('');
  protected readonly role = computed(() => this.authService.getRole() ?? 'Guest');

  ngOnInit(): void {
    if (this.authService.isLoggedIn()) {
      this.loadOverview();
    }
  }

  protected loadOverview(): void {
    this.isLoading.set(true);
    this.errorMessage.set('');

    this.analyticsService.getOverview().subscribe({
      next: (overview) => {
        this.overview.set(overview);
        this.isLoading.set(false);
      },
      error: () => {
        this.errorMessage.set('Analytics dashboard could not be loaded. Log in and make sure the backend is running.');
        this.isLoading.set(false);
      }
    });
  }

  protected toWidth(value: unknown): number {
    const numeric = typeof value === 'number' ? value : Number(value ?? 0);
    return Math.max(12, Math.min(100, numeric * 10));
  }
}

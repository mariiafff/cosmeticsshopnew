import { CommonModule } from '@angular/common';
import { Component, inject } from '@angular/core';
import { RouterLink } from '@angular/router';
import { AuthService } from '../../services/auth.service';

@Component({
  selector: 'app-dashboard-page',
  imports: [CommonModule, RouterLink],
  templateUrl: './dashboard.html',
  styleUrl: './dashboard.css'
})
export class DashboardPage {
  private readonly authService = inject(AuthService);
  protected readonly role = this.authService.currentRole;

  protected readonly collections = [
    {
      eyebrow: 'Electronics',
      title: 'Connected Essentials',
      text: 'Explore everyday tech, practical devices, and top-rated accessories in one place.'
    },
    {
      eyebrow: 'Home & Living',
      title: 'Everyday Upgrades',
      text: 'Find useful items for your home, workspace, and routines with quick comparisons.'
    },
    {
      eyebrow: 'Fashion',
      title: 'Current Picks',
      text: 'Browse popular styles, versatile accessories, and marketplace favorites that move fast.'
    }
  ];

  protected readonly moods = ['Electronics', 'Home & Living', 'Office', 'Lifestyle'];
}

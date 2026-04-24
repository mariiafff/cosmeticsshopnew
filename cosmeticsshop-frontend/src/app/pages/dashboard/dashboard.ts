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
      eyebrow: 'Skincare',
      title: 'Radiance Ritual',
      text: 'Serums, creams, and daily essentials for skin that looks rested and luminous.'
    },
    {
      eyebrow: 'Makeup',
      title: 'Soft Couture',
      text: 'Velvet lips, clean complexion, satin blush, and softly defined eyes.'
    },
    {
      eyebrow: 'Fragrance',
      title: 'Private Scent',
      text: 'Elegant notes for morning, evening, and everything between.'
    }
  ];

  protected readonly moods = ['Clean skincare', 'Soft glam makeup', 'Signature scents', 'Hair essentials'];
}

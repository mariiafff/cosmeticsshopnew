import { Component, OnInit, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';

import { User, UsersService } from '../../services/users.service';

@Component({
  selector: 'app-admin-users-page',
  standalone: true,
  imports: [CommonModule, RouterLink],
  templateUrl: './admin-users.html',
  styleUrl: './admin-users.css'
})
export class AdminUsersPage implements OnInit {
  private readonly usersService = inject(UsersService);

  protected readonly users = signal<User[]>([]);
  protected readonly isLoading = signal(false);
  protected readonly errorMessage = signal('');
  protected readonly successMessage = signal('');

  ngOnInit(): void {
    this.loadUsers();
  }

  protected loadUsers(): void {
    this.isLoading.set(true);
    this.errorMessage.set('');
    this.successMessage.set('');

    this.usersService.getUsers().subscribe({
      next: (users) => {
        this.users.set(users);
        this.isLoading.set(false);
      },
      error: () => {
        this.errorMessage.set('Could not load users.');
        this.isLoading.set(false);
      }
    });
  }

  protected toggleStatus(user: User): void {
    const nextStatus = user.status === 'ACTIVE' ? 'INACTIVE' : 'ACTIVE';
    this.usersService.updateUserStatus(user.id, nextStatus).subscribe({
      next: () => {
        this.successMessage.set(`Updated ${user.email} to ${nextStatus}.`);
        this.loadUsers();
      },
      error: () => {
        this.errorMessage.set('Could not change user status.');
      }
    });
  }

  protected deleteUser(user: User): void {
    this.usersService.deleteUser(user.id).subscribe({
      next: () => {
        this.successMessage.set(`Deleted ${user.email}.`);
        this.users.update((list) => list.filter((item) => item.id !== user.id));
      },
      error: () => {
        this.errorMessage.set('Unable to delete user.');
      }
    });
  }
}

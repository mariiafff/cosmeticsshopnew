import { Routes } from '@angular/router';
import { AuthGuard } from './guards/auth.guard';
import { RoleGuard } from './guards/role.guard';

export const routes: Routes = [
  {
    path: '',
    pathMatch: 'full',
    redirectTo: 'dashboard'
  },
  {
    path: 'dashboard',
    loadComponent: () =>
      import('./pages/dashboard/dashboard').then((m) => m.DashboardPage)
  },
  {
    path: 'login',
    loadComponent: () => import('./pages/login/login').then((m) => m.LoginPage)
  },
  {
    path: 'register',
    loadComponent: () => import('./pages/register/register').then((m) => m.RegisterPage)
  },
  {
    path: 'products',
    loadComponent: () => import('./pages/products/products').then((m) => m.ProductsPage),
    canActivate: [AuthGuard]
  },
  {
    path: 'orders',
    loadComponent: () => import('./pages/orders/orders').then((m) => m.OrdersPage),
    canActivate: [AuthGuard, RoleGuard],
    data: { roles: ['INDIVIDUAL'] }
  },
  {
    path: 'manage/products',
    loadComponent: () =>
      import('./pages/manage-products/manage-products').then((m) => m.ManageProductsPage),
    canActivate: [AuthGuard, RoleGuard],
    data: { roles: ['CORPORATE', 'ADMIN'] }
  },
  {
    path: 'manage/orders',
    loadComponent: () =>
      import('./pages/manage-orders/manage-orders').then((m) => m.ManageOrdersPage),
    canActivate: [AuthGuard, RoleGuard],
    data: { roles: ['CORPORATE', 'ADMIN'] }
  },
  {
    path: 'admin/users',
    loadComponent: () => import('./pages/admin-users/admin-users').then((m) => m.AdminUsersPage),
    canActivate: [AuthGuard, RoleGuard],
    data: { roles: ['ADMIN'] }
  },
  {
    path: 'admin/stores',
    loadComponent: () => import('./pages/admin-stores/admin-stores').then((m) => m.AdminStoresPage),
    canActivate: [AuthGuard, RoleGuard],
    data: { roles: ['ADMIN'] }
  },
  {
    path: 'admin/categories',
    loadComponent: () =>
      import('./pages/admin-categories/admin-categories').then((m) => m.AdminCategoriesPage),
    canActivate: [AuthGuard, RoleGuard],
    data: { roles: ['ADMIN'] }
  },
  {
    path: 'orders/payment-success',
    loadComponent: () => import('./pages/orders/orders').then((m) => m.OrdersPage),
    canActivate: [AuthGuard, RoleGuard],
    data: { roles: ['INDIVIDUAL'] }
  },
  {
    path: 'checkout/payment-success',
    loadComponent: () => import('./pages/checkout/checkout').then((m) => m.CheckoutPage)
  },
  {
    path: 'checkout',
    loadComponent: () => import('./pages/checkout/checkout').then((m) => m.CheckoutPage),
    canActivate: [AuthGuard, RoleGuard],
    data: { roles: ['INDIVIDUAL'] }
  },
  {
    path: 'chat',
    loadComponent: () => import('./pages/chat/chat').then((m) => m.ChatPage),
    canActivate: [AuthGuard]
  },
  {
    path: '**',
    redirectTo: 'dashboard'
  }
];

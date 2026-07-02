import { Routes } from '@angular/router';

import { authGuard } from './core/guards/auth.guard';
import { publicGuard } from './core/guards/public.guard';
import { roleGuard } from './core/guards/role.guard';
import { PrivateLayoutComponent } from './layout/private-layout/private-layout.component';
import { PublicLayoutComponent } from './layout/public-layout/public-layout.component';

export const routes: Routes = [
  { path: '', pathMatch: 'full', redirectTo: 'login' },
  {
    path: 'login',
    canActivate: [publicGuard],
    component: PublicLayoutComponent,
    children: [
      {
        path: '',
        loadComponent: () =>
          import('./features/auth/login/login.component').then((m) => m.LoginComponent),
      },
    ],
  },
  {
    path: 'app',
    canActivate: [authGuard],
    component: PrivateLayoutComponent,
    children: [
      { path: '', pathMatch: 'full', redirectTo: 'dashboard' },
      {
        path: 'dashboard',
        loadComponent: () =>
          import('./features/dashboard/dashboard.component').then((m) => m.DashboardComponent),
      },
      {
        path: 'products',
        canActivate: [roleGuard(['ADMIN', 'INVENTARIO'])],
        loadComponent: () =>
          import('./features/products/products.component').then((m) => m.ProductsComponent),
      },
      {
        path: 'inventory',
        canActivate: [roleGuard(['ADMIN', 'INVENTARIO'])],
        loadComponent: () =>
          import('./features/inventory/inventory.component').then((m) => m.InventoryComponent),
      },
      { path: 'sales', pathMatch: 'full', redirectTo: 'sales/pos' },
      {
        path: 'sales/pos',
        canActivate: [roleGuard(['ADMIN', 'CAJERO'])],
        loadComponent: () => import('./features/sales/pos.component').then((m) => m.PosComponent),
      },
      {
        path: 'purchases',
        canActivate: [roleGuard(['ADMIN', 'INVENTARIO'])],
        loadComponent: () =>
          import('./features/purchases/purchases.component').then((m) => m.PurchasesComponent),
      },
      {
        path: 'customers',
        canActivate: [roleGuard(['ADMIN', 'CAJERO'])],
        loadComponent: () =>
          import('./features/customers/customers.component').then((m) => m.CustomersComponent),
      },
      {
        path: 'suppliers',
        canActivate: [roleGuard(['ADMIN', 'INVENTARIO'])],
        loadComponent: () =>
          import('./features/suppliers/suppliers.component').then((m) => m.SuppliersComponent),
      },
      {
        path: 'cash',
        canActivate: [roleGuard(['ADMIN', 'CAJERO', 'REPORTES'])],
        loadComponent: () => import('./features/cash/cash.component').then((m) => m.CashComponent),
      },
      {
        path: 'reports',
        canActivate: [roleGuard(['ADMIN', 'REPORTES'])],
        loadComponent: () =>
          import('./features/reports/reports.component').then((m) => m.ReportsComponent),
      },
      {
        path: 'users',
        canActivate: [roleGuard(['ADMIN'])],
        loadComponent: () => import('./features/users/users.component').then((m) => m.UsersComponent),
      },
    ],
  },
  { path: '**', redirectTo: 'login' },
];

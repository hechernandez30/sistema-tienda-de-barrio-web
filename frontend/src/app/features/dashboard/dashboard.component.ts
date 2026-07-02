import { Component, computed, inject } from '@angular/core';
import { RouterLink } from '@angular/router';

import { AuthService } from '../../core/services/auth.service';

interface DashboardCard {
  title: string;
  description: string;
  icon: string;
  route: string;
  accent: string;
}

@Component({
  selector: 'app-dashboard',
  standalone: true,
  imports: [RouterLink],
  templateUrl: './dashboard.component.html',
})
export class DashboardComponent {
  private readonly authService = inject(AuthService);

  readonly firstName = computed(() => this.authService.currentUser()?.firstName ?? '');
  readonly role = computed(() => this.authService.currentUser()?.role ?? '');

  readonly cards: DashboardCard[] = [
    { title: 'Ventas del día', description: 'Resumen de ventas de hoy', icon: 'point_of_sale', route: '/app/sales/pos', accent: 'bg-emerald-100 text-emerald-700' },
    { title: 'Caja actual', description: 'Estado de la caja abierta', icon: 'payments', route: '/app/cash', accent: 'bg-amber-100 text-amber-700' },
    { title: 'Stock bajo', description: 'Productos por reabastecer', icon: 'warning', route: '/app/inventory', accent: 'bg-rose-100 text-rose-700' },
    { title: 'Productos', description: 'Catálogo de productos', icon: 'inventory_2', route: '/app/products', accent: 'bg-blue-100 text-blue-700' },
    { title: 'Compras', description: 'Órdenes de compra', icon: 'shopping_cart', route: '/app/purchases', accent: 'bg-indigo-100 text-indigo-700' },
    { title: 'Reportes', description: 'Indicadores del negocio', icon: 'bar_chart', route: '/app/reports', accent: 'bg-cyan-100 text-cyan-700' },
  ];
}

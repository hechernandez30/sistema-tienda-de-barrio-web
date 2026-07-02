export interface NavItem {
  label: string;
  icon: string;
  route: string;
  /** Roles que pueden ver la opción. ADMIN se incluye explícitamente en cada una. */
  roles: string[];
}

export const NAV_ITEMS: NavItem[] = [
  { label: 'Dashboard', icon: 'dashboard', route: '/app/dashboard', roles: ['ADMIN', 'CAJERO', 'INVENTARIO', 'REPORTES'] },
  { label: 'Productos', icon: 'inventory_2', route: '/app/products', roles: ['ADMIN', 'INVENTARIO'] },
  { label: 'Inventario', icon: 'warehouse', route: '/app/inventory', roles: ['ADMIN', 'INVENTARIO'] },
  { label: 'Ventas / POS', icon: 'point_of_sale', route: '/app/sales/pos', roles: ['ADMIN', 'CAJERO'] },
  { label: 'Compras', icon: 'shopping_cart', route: '/app/purchases', roles: ['ADMIN', 'INVENTARIO'] },
  { label: 'Clientes', icon: 'groups', route: '/app/customers', roles: ['ADMIN', 'CAJERO'] },
  { label: 'Proveedores', icon: 'local_shipping', route: '/app/suppliers', roles: ['ADMIN', 'INVENTARIO'] },
  { label: 'Caja', icon: 'payments', route: '/app/cash', roles: ['ADMIN', 'CAJERO', 'REPORTES'] },
  { label: 'Reportes', icon: 'bar_chart', route: '/app/reports', roles: ['ADMIN', 'REPORTES'] },
  { label: 'Usuarios y Roles', icon: 'manage_accounts', route: '/app/users', roles: ['ADMIN'] },
];

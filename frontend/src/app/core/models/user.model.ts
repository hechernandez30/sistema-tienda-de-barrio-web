export interface Role {
  id: string;
  name: string;
  description?: string | null;
  active: boolean;
}

/** Fila del listado de usuarios (GET /api/users). */
export interface UserListItem {
  id: string;
  roleName?: string | null;
  username: string;
  email?: string | null;
  firstName: string;
  lastName: string;
  active: boolean;
  lastLoginAt?: string | null;
}

/** Detalle de usuario (GET /api/users/{id}). */
export interface UserDetail {
  id: string;
  role?: Role | null;
  username: string;
  email?: string | null;
  firstName: string;
  lastName: string;
  active: boolean;
  lastLoginAt?: string | null;
  createdAt?: string | null;
  updatedAt?: string | null;
}

export interface UserCreateRequest {
  roleId: string;
  username: string;
  email?: string | null;
  password: string;
  firstName: string;
  lastName: string;
  active: boolean;
}

export interface UserUpdateRequest {
  roleId: string;
  email?: string | null;
  firstName: string;
  lastName: string;
  active: boolean;
}

export interface ChangePasswordRequest {
  newPassword: string;
}

/** Descripciones funcionales de los roles conocidos. */
export const ROLE_DESCRIPTIONS: Record<string, string> = {
  ADMIN: 'Acceso completo al sistema.',
  CAJERO: 'Acceso a ventas, clientes y caja.',
  INVENTARIO: 'Acceso a productos, inventario, compras y proveedores.',
  REPORTES: 'Acceso a reportes administrativos.',
};

/** Explicación extendida usada en el detalle del rol. */
export const ROLE_EXPLANATIONS: Record<string, string> = {
  ADMIN: 'ADMIN puede administrar usuarios, roles y operar todos los módulos del sistema.',
  CAJERO: 'CAJERO puede registrar ventas, consultar clientes y operar caja.',
  INVENTARIO: 'INVENTARIO puede gestionar productos, inventario, compras y proveedores.',
  REPORTES: 'REPORTES puede consultar reportes administrativos y de caja.',
};

export type RoleName = 'ADMIN' | 'CAJERO' | 'INVENTARIO' | 'REPORTES';

export interface AuthUser {
  id: string;
  username: string;
  email: string | null;
  firstName: string;
  lastName: string;
  role: string;
  active?: boolean;
  lastLoginAt?: string | null;
}

export interface LoginResponse {
  accessToken: string;
  tokenType: string;
  expiresIn: number;
  user: {
    id: string;
    username: string;
    email: string | null;
    firstName: string;
    lastName: string;
    role: string;
  };
}

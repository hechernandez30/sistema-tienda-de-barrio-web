export interface CustomerListItem {
  id: string;
  fullName: string;
  nit?: string | null;
  phone?: string | null;
  email?: string | null;
  active: boolean;
}

export interface CustomerDetail {
  id: string;
  fullName: string;
  nit?: string | null;
  phone?: string | null;
  email?: string | null;
  address?: string | null;
  active: boolean;
  createdAt?: string;
  updatedAt?: string;
}

/**
 * Payload de creación/actualización. El backend espera `isActive` en el body,
 * aunque en las respuestas expone `active`.
 */
export interface CustomerRequest {
  fullName: string;
  nit?: string | null;
  phone?: string | null;
  email?: string | null;
  address?: string | null;
  isActive: boolean;
}

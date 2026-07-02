export interface SupplierListItem {
  id: string;
  name: string;
  nit?: string | null;
  contactName?: string | null;
  phone?: string | null;
  email?: string | null;
  active: boolean;
}

export interface SupplierDetail {
  id: string;
  name: string;
  nit?: string | null;
  contactName?: string | null;
  phone?: string | null;
  email?: string | null;
  address?: string | null;
  active: boolean;
  createdAt?: string;
  updatedAt?: string;
}

/**
 * Payload de creación/actualización. El backend espera la propiedad `isActive`
 * en el body (aunque en las respuestas la expone como `active`).
 */
export interface SupplierRequest {
  name: string;
  nit?: string | null;
  contactName?: string | null;
  phone?: string | null;
  email?: string | null;
  address?: string | null;
  isActive: boolean;
}

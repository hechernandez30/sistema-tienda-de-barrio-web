export type PurchaseStatus = 'DRAFT' | 'CONFIRMED' | 'CANCELLED';
export type PaymentMethod = 'CASH' | 'TRANSFER' | 'CARD';

export const PURCHASE_STATUS_LABELS: Record<PurchaseStatus, string> = {
  DRAFT: 'Borrador',
  CONFIRMED: 'Confirmada',
  CANCELLED: 'Cancelada',
};

export const PAYMENT_METHOD_LABELS: Record<PaymentMethod, string> = {
  CASH: 'Efectivo',
  TRANSFER: 'Transferencia',
  CARD: 'Tarjeta',
};

export interface PurchaseListItem {
  id: string;
  purchaseNumber?: number | null;
  supplierName?: string | null;
  purchaseDate: string;
  status: PurchaseStatus;
  paid: boolean;
  total: number;
  createdAt?: string;
}

export interface PurchaseItemDetail {
  id?: string;
  productId: string;
  productName?: string | null;
  barcode?: string | null;
  quantity: number;
  unitCost: number;
  lineTotal: number;
  expirationDate?: string | null;
  lotCode?: string | null;
}

export interface PurchaseSupplierSummary {
  id: string;
  name: string;
  nit?: string | null;
}

export interface PurchaseDetail {
  id: string;
  purchaseNumber?: number | null;
  supplier?: PurchaseSupplierSummary | null;
  purchaseDate: string;
  status: PurchaseStatus;
  paid: boolean;
  paymentMethod?: PaymentMethod | null;
  subtotal: number;
  discountTotal: number;
  taxTotal: number;
  total: number;
  notes?: string | null;
  items: PurchaseItemDetail[];
  createdAt?: string;
}

export interface PurchaseItemRequest {
  productId: string;
  quantity: number;
  unitCost: number;
  expirationDate?: string | null;
  lotCode?: string | null;
}

export interface PurchaseCreateRequest {
  supplierId: string;
  purchaseDate?: string | null;
  isPaid: boolean;
  paymentMethod?: PaymentMethod | null;
  notes?: string | null;
  items: PurchaseItemRequest[];
}

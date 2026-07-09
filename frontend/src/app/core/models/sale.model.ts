export type PaymentMethod = 'CASH' | 'TRANSFER' | 'CARD';
export type SaleStatus = 'COMPLETED' | 'CANCELLED';

export const PAYMENT_METHOD_LABELS: Record<PaymentMethod, string> = {
  CASH: 'Efectivo',
  TRANSFER: 'Transferencia',
  CARD: 'Tarjeta',
};

export const SALE_STATUS_LABELS: Record<SaleStatus, string> = {
  COMPLETED: 'Completada',
  CANCELLED: 'Cancelada',
};

export interface SaleListItem {
  id: string;
  saleNumber?: number | null;
  customerName?: string | null;
  cashierName?: string | null;
  saleDate: string;
  paymentMethod: PaymentMethod;
  total: number;
  status: SaleStatus;
}

export interface SaleItemDetail {
  productId: string;
  barcode?: string | null;
  productName: string;
  quantity: number;
  unitPrice: number;
  discountAmount: number;
  lineTotal: number;
}

export interface SaleCustomerSummary {
  id: string;
  fullName: string;
  nit?: string | null;
}

export interface SaleCashierSummary {
  id: string;
  username: string;
  fullName?: string | null;
}

export interface SaleDetail {
  id: string;
  saleNumber?: number | null;
  customer?: SaleCustomerSummary | null;
  cashier?: SaleCashierSummary | null;
  cashSessionId?: string | null;
  saleDate: string;
  paymentMethod: PaymentMethod;
  subtotal: number;
  discountTotal: number;
  taxTotal: number;
  total: number;
  status: SaleStatus;
  notes?: string | null;
  items: SaleItemDetail[];
  createdAt?: string;
}

export interface SaleItemRequest {
  productId: string;
  quantity: number;
}

export interface SaleCreateRequest {
  customerId?: string | null;
  paymentMethod: PaymentMethod;
  notes?: string | null;
  items: SaleItemRequest[];
}

/** Línea del carrito en memoria (no se persiste hasta registrar la venta). */
export interface PosCartItem {
  productId: string;
  barcode: string;
  productName: string;
  salePrice: number;
  availableStock: number;
  tracksExpiration?: boolean;
  quantity: number;
}

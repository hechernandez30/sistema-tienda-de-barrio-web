export type CashSessionStatus = 'OPEN' | 'CLOSED';
export type CashMovementType = 'INCOME' | 'EXPENSE';
export type PaymentMethod = 'CASH' | 'TRANSFER' | 'CARD';

export const CASH_MOVEMENT_TYPE_LABELS: Record<CashMovementType, string> = {
  INCOME: 'Ingreso',
  EXPENSE: 'Egreso',
};

export const PAYMENT_METHOD_LABELS: Record<PaymentMethod, string> = {
  CASH: 'Efectivo',
  TRANSFER: 'Transferencia',
  CARD: 'Tarjeta',
};

export const CASH_SESSION_STATUS_LABELS: Record<CashSessionStatus, string> = {
  OPEN: 'Abierta',
  CLOSED: 'Cerrada',
};

/** Categorías sugeridas para movimientos manuales. */
export const INCOME_CATEGORIES: string[] = ['AJUSTE_CAJA', 'OTRO_INGRESO'];
export const EXPENSE_CATEGORIES: string[] = [
  'PAGO_SERVICIO',
  'RETIRO',
  'TRANSPORTE',
  'AJUSTE_CAJA',
  'OTRO_EGRESO',
];

export interface CashSession {
  id: string;
  openedBy?: string | null;
  closedBy?: string | null;
  openedAt: string;
  closedAt?: string | null;
  openingAmount: number;
  expectedAmount: number;
  countedAmount?: number | null;
  differenceAmount?: number | null;
  status: CashSessionStatus;
  notes?: string | null;
}

export interface CashMovement {
  id: string;
  cashSessionId?: string | null;
  movementType: CashMovementType;
  category: string;
  paymentMethod: PaymentMethod;
  amount: number;
  description?: string | null;
  referenceSaleId?: string | null;
  referencePurchaseId?: string | null;
  createdAt: string;
  createdBy?: string | null;
}

export interface CurrentCashSummary {
  cashSessionId?: string;
  status?: CashSessionStatus;
  openedAt?: string;
  openedBy?: string;
  openingAmount?: number;
  totalCashIncome?: number;
  totalTransferIncome?: number;
  totalCardIncome?: number;
  totalExpenses?: number;
  expectedAmount?: number;
  movementCount?: number;
}

export interface OpenCashSessionRequest {
  openingAmount: number;
  notes?: string | null;
}

export interface CloseCashSessionRequest {
  countedAmount: number;
  notes?: string | null;
}

export interface CreateCashMovementRequest {
  cashSessionId?: string | null;
  movementType: CashMovementType;
  category: string;
  paymentMethod: PaymentMethod;
  amount: number;
  description?: string | null;
}

export type PaymentMethod = 'CASH' | 'TRANSFER' | 'CARD';
export type CashMovementType = 'INCOME' | 'EXPENSE';

export const PAYMENT_METHOD_LABELS: Record<PaymentMethod, string> = {
  CASH: 'Efectivo',
  TRANSFER: 'Transferencia',
  CARD: 'Tarjeta',
};

export const MOVEMENT_TYPE_LABELS: Record<CashMovementType, string> = {
  INCOME: 'Ingreso',
  EXPENSE: 'Egreso',
};

export interface SalesSummaryReport {
  fromDate?: string | null;
  toDate?: string | null;
  totalSales: number;
  salesCount: number;
  cancelledSalesCount: number;
  averageSaleAmount: number;
}

export interface SalesByPaymentMethodReport {
  paymentMethod: PaymentMethod;
  salesCount: number;
  totalAmount: number;
}

export interface DailySalesReport {
  date: string;
  salesCount: number;
  totalAmount: number;
}

export interface TopProductReport {
  productId: string;
  barcode?: string | null;
  productName: string;
  quantitySold: number;
  totalAmount: number;
}

export interface LowStockReport {
  productId: string;
  barcode?: string | null;
  productName: string;
  currentStock: number;
  minStock: number;
  categoryName?: string | null;
}

export interface InventorySummaryReport {
  totalProducts: number;
  activeProducts: number;
  lowStockProducts: number;
  totalInventoryCost: number;
  totalInventorySaleValue: number;
}

export interface PurchasesSummaryReport {
  fromDate?: string | null;
  toDate?: string | null;
  purchaseCount: number;
  confirmedPurchaseCount: number;
  cancelledPurchaseCount: number;
  totalPurchasedAmount: number;
}

export interface PurchasesBySupplierReport {
  supplierId: string;
  supplierName: string;
  purchaseCount: number;
  totalAmount: number;
}

export interface CashSummaryReport {
  fromDate?: string | null;
  toDate?: string | null;
  totalCashIncome: number;
  totalTransferIncome: number;
  totalCardIncome: number;
  totalIncome: number;
  totalExpenses: number;
  netAmount: number;
}

export interface CashByCategoryReport {
  movementType: CashMovementType;
  category: string;
  paymentMethod: PaymentMethod;
  movementCount: number;
  totalAmount: number;
}

export interface EstimatedProfitReport {
  fromDate?: string | null;
  toDate?: string | null;
  totalSales: number;
  estimatedCost: number;
  estimatedGrossProfit: number;
}

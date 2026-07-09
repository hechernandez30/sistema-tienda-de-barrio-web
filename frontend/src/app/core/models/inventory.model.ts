export type InventoryMovementType =
  | 'PURCHASE'
  | 'SALE'
  | 'ADJUSTMENT_IN'
  | 'ADJUSTMENT_OUT'
  | 'SALE_CANCEL'
  | 'PURCHASE_CANCEL';

export interface InventoryMovement {
  id: string;
  productId: string;
  productName?: string | null;
  barcode?: string | null;
  movementType: InventoryMovementType;
  quantity: number;
  previousStock: number;
  newStock: number;
  unitCost?: number | null;
  notes?: string | null;
  createdBy?: string | null;
  createdAt: string;
}

export interface InventoryAdjustmentRequest {
  productId: string;
  quantity: number;
  unitCost?: number | null;
  expirationDate?: string | null;
  lotCode?: string | null;
  notes?: string | null;
}

export interface ProductLot {
  id: string;
  productId: string;
  productName: string;
  barcode?: string | null;
  lotCode?: string | null;
  expirationDate: string;
  daysToExpire: number;
  quantity: number;
  unitCost?: number | null;
  expired: boolean;
}

export interface LowStockProduct {
  productId: string;
  barcode: string;
  name: string;
  currentStock: number;
  minStock: number;
  missingQuantity: number;
  unitMeasureName?: string | null;
}

/** Movimientos que incrementan stock vs. los que lo disminuyen. */
export const INCOMING_MOVEMENT_TYPES: InventoryMovementType[] = ['PURCHASE', 'ADJUSTMENT_IN', 'SALE_CANCEL'];
export const OUTGOING_MOVEMENT_TYPES: InventoryMovementType[] = ['SALE', 'ADJUSTMENT_OUT', 'PURCHASE_CANCEL'];

export const MOVEMENT_TYPE_LABELS: Record<InventoryMovementType, string> = {
  PURCHASE: 'Compra',
  SALE: 'Venta',
  ADJUSTMENT_IN: 'Ajuste entrada',
  ADJUSTMENT_OUT: 'Ajuste salida',
  SALE_CANCEL: 'Anulación venta',
  PURCHASE_CANCEL: 'Anulación compra',
};

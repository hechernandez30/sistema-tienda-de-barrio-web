export interface ProductListItem {
  id: string;
  barcode: string;
  sku?: string | null;
  name: string;
  categoryName?: string | null;
  unitMeasureName?: string | null;
  salePrice: number;
  currentStock: number;
  minStock: number;
  active: boolean;
}

export interface ProductDetail {
  id: string;
  barcode: string;
  sku?: string | null;
  name: string;
  description?: string | null;
  categoryId?: string | null;
  categoryName?: string | null;
  unitMeasureId?: string | null;
  unitMeasureName?: string | null;
  purchasePrice: number;
  salePrice: number;
  minStock: number;
  currentStock: number;
  active: boolean;
  createdAt?: string;
  updatedAt?: string;
}

/** Resultado del endpoint POS (búsqueda por nombre / código de barras). */
export interface ProductPos {
  id: string;
  barcode: string;
  name: string;
  salePrice: number;
  currentStock: number;
  unitMeasureName?: string | null;
}

/** Payload de creación: el backend acepta currentStock inicial. */
export interface ProductCreatePayload {
  barcode: string;
  sku?: string | null;
  name: string;
  description?: string | null;
  categoryId?: string | null;
  unitMeasureId?: string | null;
  purchasePrice: number;
  salePrice: number;
  minStock: number;
  currentStock: number;
  active: boolean;
}

/** Payload de actualización: el stock actual se gestiona desde Inventario, no aquí. */
export interface ProductUpdatePayload {
  barcode: string;
  sku?: string | null;
  name: string;
  description?: string | null;
  categoryId?: string | null;
  unitMeasureId?: string | null;
  purchasePrice: number;
  salePrice: number;
  minStock: number;
  active: boolean;
}

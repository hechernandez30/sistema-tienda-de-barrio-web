export interface Category {
  id: string;
  name: string;
  description?: string | null;
  active: boolean;
}

export interface CategoryCreatePayload {
  name: string;
  description?: string | null;
}

export interface UnitMeasure {
  id: string;
  code: string;
  name: string;
  active: boolean;
}

export interface UnitMeasureCreatePayload {
  code: string;
  name: string;
}

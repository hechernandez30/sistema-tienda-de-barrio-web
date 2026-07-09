-- ============================================================
-- Migración v2: Lotes y fechas de vencimiento
-- Ejecutar en bases existentes (bd_tienda / tienda_barrio_db).
-- ============================================================

ALTER TABLE products
    ADD COLUMN IF NOT EXISTS tracks_expiration BOOLEAN NOT NULL DEFAULT FALSE;

CREATE TABLE IF NOT EXISTS product_lots (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    product_id UUID NOT NULL REFERENCES products(id),
    lot_code VARCHAR(50),
    expiration_date DATE NOT NULL,
    quantity NUMERIC(12,3) NOT NULL DEFAULT 0,
    unit_cost NUMERIC(12,2),
    received_at TIMESTAMP NOT NULL DEFAULT NOW(),
    purchase_id UUID REFERENCES purchases(id),
    purchase_item_id UUID REFERENCES purchase_items(id),
    notes TEXT,
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP,
    deleted_at TIMESTAMP,
    CONSTRAINT ck_product_lots_quantity CHECK (quantity >= 0)
);

CREATE INDEX IF NOT EXISTS idx_product_lots_product ON product_lots (product_id);
CREATE INDEX IF NOT EXISTS idx_product_lots_expiration ON product_lots (expiration_date);
CREATE INDEX IF NOT EXISTS idx_product_lots_product_expiration
    ON product_lots (product_id, expiration_date)
    WHERE is_deleted = FALSE AND quantity > 0;

ALTER TABLE purchase_items
    ADD COLUMN IF NOT EXISTS expiration_date DATE,
    ADD COLUMN IF NOT EXISTS lot_code VARCHAR(50);

ALTER TABLE inventory_movements
    ADD COLUMN IF NOT EXISTS lot_id UUID REFERENCES product_lots(id);

CREATE TABLE IF NOT EXISTS sale_lot_allocations (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    sale_id UUID NOT NULL REFERENCES sales(id),
    sale_item_id UUID NOT NULL REFERENCES sale_items(id),
    product_lot_id UUID NOT NULL REFERENCES product_lots(id),
    quantity NUMERIC(12,3) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    CONSTRAINT ck_sale_lot_allocations_quantity CHECK (quantity > 0)
);

CREATE INDEX IF NOT EXISTS idx_sale_lot_allocations_sale ON sale_lot_allocations (sale_id);
CREATE INDEX IF NOT EXISTS idx_sale_lot_allocations_sale_item ON sale_lot_allocations (sale_item_id);

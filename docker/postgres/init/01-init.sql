-- ============================================================
-- Sistema Tienda de Barrio Web
-- Base de datos PostgreSQL
-- Versión inicial
-- ============================================================

CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
CREATE EXTENSION IF NOT EXISTS pgcrypto;

-- ============================================================
-- Tipos ENUM
-- ============================================================

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_type WHERE typname = 'payment_method') THEN
        CREATE TYPE payment_method AS ENUM ('CASH', 'TRANSFER', 'CARD');
    END IF;

    IF NOT EXISTS (SELECT 1 FROM pg_type WHERE typname = 'inventory_movement_type') THEN
        CREATE TYPE inventory_movement_type AS ENUM ('PURCHASE', 'SALE', 'ADJUSTMENT_IN', 'ADJUSTMENT_OUT', 'SALE_CANCEL', 'PURCHASE_CANCEL');
    END IF;

    IF NOT EXISTS (SELECT 1 FROM pg_type WHERE typname = 'cash_movement_type') THEN
        CREATE TYPE cash_movement_type AS ENUM ('INCOME', 'EXPENSE');
    END IF;

    IF NOT EXISTS (SELECT 1 FROM pg_type WHERE typname = 'cash_session_status') THEN
        CREATE TYPE cash_session_status AS ENUM ('OPEN', 'CLOSED');
    END IF;

    IF NOT EXISTS (SELECT 1 FROM pg_type WHERE typname = 'sale_status') THEN
        CREATE TYPE sale_status AS ENUM ('COMPLETED', 'CANCELLED');
    END IF;

    IF NOT EXISTS (SELECT 1 FROM pg_type WHERE typname = 'purchase_status') THEN
        CREATE TYPE purchase_status AS ENUM ('DRAFT', 'CONFIRMED', 'CANCELLED');
    END IF;
END $$;

-- ============================================================
-- Seguridad: roles, permisos y usuarios
-- ============================================================

CREATE TABLE IF NOT EXISTS roles (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    name VARCHAR(50) NOT NULL UNIQUE,
    description VARCHAR(255),
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP,
    deleted_at TIMESTAMP
);

CREATE TABLE IF NOT EXISTS permissions (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    code VARCHAR(100) NOT NULL UNIQUE,
    description VARCHAR(255),
    created_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS role_permissions (
    role_id UUID NOT NULL REFERENCES roles(id),
    permission_id UUID NOT NULL REFERENCES permissions(id),
    PRIMARY KEY (role_id, permission_id)
);

CREATE TABLE IF NOT EXISTS app_users (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    role_id UUID NOT NULL REFERENCES roles(id),
    username VARCHAR(50) NOT NULL UNIQUE,
    email VARCHAR(120) UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    first_name VARCHAR(80) NOT NULL,
    last_name VARCHAR(80) NOT NULL,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    last_login_at TIMESTAMP,
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP,
    deleted_at TIMESTAMP,
    created_by UUID REFERENCES app_users(id),
    updated_by UUID REFERENCES app_users(id),
    deleted_by UUID REFERENCES app_users(id)
);

-- ============================================================
-- Catálogos
-- ============================================================

CREATE TABLE IF NOT EXISTS product_categories (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    name VARCHAR(100) NOT NULL,
    description VARCHAR(255),
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP,
    deleted_at TIMESTAMP,
    created_by UUID REFERENCES app_users(id),
    updated_by UUID REFERENCES app_users(id),
    deleted_by UUID REFERENCES app_users(id),
    CONSTRAINT uq_product_categories_name_active UNIQUE (name, is_deleted)
);

CREATE TABLE IF NOT EXISTS unit_measures (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    code VARCHAR(20) NOT NULL,
    name VARCHAR(80) NOT NULL,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP,
    deleted_at TIMESTAMP,
    CONSTRAINT uq_unit_measures_code_active UNIQUE (code, is_deleted)
);

CREATE TABLE IF NOT EXISTS products (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    category_id UUID REFERENCES product_categories(id),
    unit_measure_id UUID REFERENCES unit_measures(id),
    barcode VARCHAR(80) NOT NULL,
    sku VARCHAR(80),
    name VARCHAR(160) NOT NULL,
    description TEXT,
    purchase_price NUMERIC(12,2) NOT NULL DEFAULT 0,
    sale_price NUMERIC(12,2) NOT NULL DEFAULT 0,
    min_stock NUMERIC(12,3) NOT NULL DEFAULT 0,
    current_stock NUMERIC(12,3) NOT NULL DEFAULT 0,
    tracks_expiration BOOLEAN NOT NULL DEFAULT FALSE,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP,
    deleted_at TIMESTAMP,
    created_by UUID REFERENCES app_users(id),
    updated_by UUID REFERENCES app_users(id),
    deleted_by UUID REFERENCES app_users(id),
    CONSTRAINT ck_products_prices CHECK (purchase_price >= 0 AND sale_price >= 0),
    CONSTRAINT ck_products_stock CHECK (min_stock >= 0 AND current_stock >= 0)
);

CREATE UNIQUE INDEX IF NOT EXISTS uq_products_barcode_not_deleted
ON products (barcode)
WHERE is_deleted = FALSE;

CREATE UNIQUE INDEX IF NOT EXISTS uq_products_sku_not_deleted
ON products (sku)
WHERE is_deleted = FALSE AND sku IS NOT NULL;

CREATE INDEX IF NOT EXISTS idx_products_name ON products (name);
CREATE INDEX IF NOT EXISTS idx_products_barcode ON products (barcode);
CREATE INDEX IF NOT EXISTS idx_products_name_lower
ON products (LOWER(name))
WHERE is_deleted = FALSE;

-- ============================================================
-- Clientes y proveedores
-- ============================================================

CREATE TABLE IF NOT EXISTS customers (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    full_name VARCHAR(160) NOT NULL,
    nit VARCHAR(30),
    phone VARCHAR(30),
    email VARCHAR(120),
    address TEXT,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP,
    deleted_at TIMESTAMP,
    created_by UUID REFERENCES app_users(id),
    updated_by UUID REFERENCES app_users(id),
    deleted_by UUID REFERENCES app_users(id)
);

CREATE TABLE IF NOT EXISTS suppliers (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    name VARCHAR(160) NOT NULL,
    nit VARCHAR(30),
    contact_name VARCHAR(120),
    phone VARCHAR(30),
    email VARCHAR(120),
    address TEXT,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP,
    deleted_at TIMESTAMP,
    created_by UUID REFERENCES app_users(id),
    updated_by UUID REFERENCES app_users(id),
    deleted_by UUID REFERENCES app_users(id)
);

-- ============================================================
-- Caja
-- ============================================================

CREATE TABLE IF NOT EXISTS cash_sessions (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    opened_by UUID NOT NULL REFERENCES app_users(id),
    closed_by UUID REFERENCES app_users(id),
    opened_at TIMESTAMP NOT NULL DEFAULT NOW(),
    closed_at TIMESTAMP,
    opening_amount NUMERIC(12,2) NOT NULL DEFAULT 0,
    expected_amount NUMERIC(12,2) NOT NULL DEFAULT 0,
    counted_amount NUMERIC(12,2),
    difference_amount NUMERIC(12,2),
    status cash_session_status NOT NULL DEFAULT 'OPEN',
    notes TEXT,
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP,
    deleted_at TIMESTAMP
);

CREATE UNIQUE INDEX IF NOT EXISTS uq_cash_sessions_single_open
ON cash_sessions (status)
WHERE status = 'OPEN' AND is_deleted = FALSE;

-- ============================================================
-- Ventas
-- ============================================================

CREATE TABLE IF NOT EXISTS sales (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    sale_number BIGSERIAL UNIQUE,
    customer_id UUID REFERENCES customers(id),
    cash_session_id UUID REFERENCES cash_sessions(id),
    cashier_id UUID NOT NULL REFERENCES app_users(id),
    sale_date TIMESTAMP NOT NULL DEFAULT NOW(),
    payment_method payment_method NOT NULL DEFAULT 'CASH',
    subtotal NUMERIC(12,2) NOT NULL DEFAULT 0,
    discount_total NUMERIC(12,2) NOT NULL DEFAULT 0,
    tax_total NUMERIC(12,2) NOT NULL DEFAULT 0,
    total NUMERIC(12,2) NOT NULL DEFAULT 0,
    status sale_status NOT NULL DEFAULT 'COMPLETED',
    notes TEXT,
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP,
    deleted_at TIMESTAMP,
    created_by UUID REFERENCES app_users(id),
    updated_by UUID REFERENCES app_users(id),
    deleted_by UUID REFERENCES app_users(id),
    CONSTRAINT ck_sales_amounts CHECK (subtotal >= 0 AND discount_total >= 0 AND tax_total >= 0 AND total >= 0)
);

CREATE TABLE IF NOT EXISTS sale_items (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    sale_id UUID NOT NULL REFERENCES sales(id),
    product_id UUID NOT NULL REFERENCES products(id),
    barcode VARCHAR(80) NOT NULL,
    product_name VARCHAR(160) NOT NULL,
    quantity NUMERIC(12,3) NOT NULL,
    unit_price NUMERIC(12,2) NOT NULL,
    discount_amount NUMERIC(12,2) NOT NULL DEFAULT 0,
    line_total NUMERIC(12,2) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    CONSTRAINT ck_sale_items_values CHECK (quantity > 0 AND unit_price >= 0 AND discount_amount >= 0 AND line_total >= 0)
);

CREATE INDEX IF NOT EXISTS idx_sales_date ON sales (sale_date);
CREATE INDEX IF NOT EXISTS idx_sales_cashier ON sales (cashier_id);
CREATE INDEX IF NOT EXISTS idx_sale_items_sale ON sale_items (sale_id);

-- ============================================================
-- Compras
-- ============================================================

CREATE TABLE IF NOT EXISTS purchases (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    purchase_number BIGSERIAL UNIQUE,
    supplier_id UUID REFERENCES suppliers(id),
    created_by_user_id UUID NOT NULL REFERENCES app_users(id),
    purchase_date TIMESTAMP NOT NULL DEFAULT NOW(),
    status purchase_status NOT NULL DEFAULT 'DRAFT',
    is_paid BOOLEAN NOT NULL DEFAULT FALSE,
    payment_method payment_method,
    subtotal NUMERIC(12,2) NOT NULL DEFAULT 0,
    discount_total NUMERIC(12,2) NOT NULL DEFAULT 0,
    tax_total NUMERIC(12,2) NOT NULL DEFAULT 0,
    total NUMERIC(12,2) NOT NULL DEFAULT 0,
    notes TEXT,
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP,
    deleted_at TIMESTAMP,
    created_by UUID REFERENCES app_users(id),
    updated_by UUID REFERENCES app_users(id),
    deleted_by UUID REFERENCES app_users(id),
    CONSTRAINT ck_purchases_amounts CHECK (subtotal >= 0 AND discount_total >= 0 AND tax_total >= 0 AND total >= 0)
);

CREATE TABLE IF NOT EXISTS purchase_items (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    purchase_id UUID NOT NULL REFERENCES purchases(id),
    product_id UUID NOT NULL REFERENCES products(id),
    quantity NUMERIC(12,3) NOT NULL,
    unit_cost NUMERIC(12,2) NOT NULL,
    line_total NUMERIC(12,2) NOT NULL,
    expiration_date DATE,
    lot_code VARCHAR(50),
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    CONSTRAINT ck_purchase_items_values CHECK (quantity > 0 AND unit_cost >= 0 AND line_total >= 0)
);

CREATE INDEX IF NOT EXISTS idx_purchases_date ON purchases (purchase_date);
CREATE INDEX IF NOT EXISTS idx_purchase_items_purchase ON purchase_items (purchase_id);

-- ============================================================
-- Lotes de producto (vencimiento)
-- ============================================================

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

-- ============================================================
-- Inventario
-- ============================================================

CREATE TABLE IF NOT EXISTS inventory_movements (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    product_id UUID NOT NULL REFERENCES products(id),
    movement_type inventory_movement_type NOT NULL,
    quantity NUMERIC(12,3) NOT NULL,
    previous_stock NUMERIC(12,3) NOT NULL,
    new_stock NUMERIC(12,3) NOT NULL,
    unit_cost NUMERIC(12,2),
    reference_sale_id UUID REFERENCES sales(id),
    reference_purchase_id UUID REFERENCES purchases(id),
    lot_id UUID REFERENCES product_lots(id),
    notes TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    created_by UUID REFERENCES app_users(id),
    CONSTRAINT ck_inventory_quantity CHECK (quantity > 0),
    CONSTRAINT ck_inventory_stock CHECK (previous_stock >= 0 AND new_stock >= 0)
);

CREATE INDEX IF NOT EXISTS idx_inventory_product ON inventory_movements (product_id);
CREATE INDEX IF NOT EXISTS idx_inventory_created_at ON inventory_movements (created_at);

-- ============================================================
-- Movimientos de caja
-- ============================================================

CREATE TABLE IF NOT EXISTS cash_movements (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    cash_session_id UUID REFERENCES cash_sessions(id),
    movement_type cash_movement_type NOT NULL,
    category VARCHAR(80) NOT NULL,
    payment_method payment_method NOT NULL DEFAULT 'CASH',
    amount NUMERIC(12,2) NOT NULL,
    description TEXT,
    reference_sale_id UUID REFERENCES sales(id),
    reference_purchase_id UUID REFERENCES purchases(id),
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    created_by UUID REFERENCES app_users(id),
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE,
    deleted_at TIMESTAMP,
    deleted_by UUID REFERENCES app_users(id),
    CONSTRAINT ck_cash_movements_amount CHECK (amount > 0)
);

CREATE INDEX IF NOT EXISTS idx_cash_movements_session ON cash_movements (cash_session_id);
CREATE INDEX IF NOT EXISTS idx_cash_movements_created_at ON cash_movements (created_at);
CREATE INDEX IF NOT EXISTS idx_cash_movements_payment_method ON cash_movements (payment_method);

-- ============================================================
-- Bitácora
-- ============================================================

CREATE TABLE IF NOT EXISTS audit_log (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id UUID REFERENCES app_users(id),
    username VARCHAR(80),
    action VARCHAR(80) NOT NULL,
    module VARCHAR(80) NOT NULL,
    entity_name VARCHAR(120),
    entity_id UUID,
    old_value JSONB,
    new_value JSONB,
    ip_address VARCHAR(80),
    user_agent TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_audit_log_user ON audit_log (user_id);
CREATE INDEX IF NOT EXISTS idx_audit_log_module ON audit_log (module);
CREATE INDEX IF NOT EXISTS idx_audit_log_created_at ON audit_log (created_at);

-- ============================================================
-- Seeds iniciales
-- Nota: cambia el password_hash desde backend usando BCrypt.
-- Este hash de ejemplo puede reemplazarse.
-- ============================================================

INSERT INTO roles (name, description)
VALUES
('ADMIN', 'Administrador del sistema'),
('CAJERO', 'Usuario encargado de ventas y caja'),
('INVENTARIO', 'Usuario encargado de productos, compras e inventario'),
('REPORTES', 'Usuario con acceso a reportes')
ON CONFLICT (name) DO NOTHING;

INSERT INTO unit_measures (code, name)
VALUES
('UND', 'Unidad'),
('LB', 'Libra'),
('KG', 'Kilogramo'),
('LT', 'Litro'),
('ML', 'Mililitro'),
('PAQ', 'Paquete')
ON CONFLICT DO NOTHING;

INSERT INTO product_categories (name, description)
VALUES
('Abarrotes', 'Productos de consumo general'),
('Bebidas', 'Bebidas frías y calientes'),
('Lácteos', 'Leche, quesos y derivados'),
('Limpieza', 'Productos de limpieza'),
('Higiene personal', 'Productos de uso personal')
ON CONFLICT DO NOTHING;

-- Usuario admin inicial.
-- Reemplazar password_hash por uno generado con BCrypt desde el backend.
INSERT INTO app_users (
    role_id,
    username,
    email,
    password_hash,
    first_name,
    last_name
)
SELECT
    r.id,
    'admin',
    'admin@local.test',
    '$2a$10$replace.this.hash.from.backend.generated.bcrypt',
    'Administrador',
    'Sistema'
FROM roles r
WHERE r.name = 'ADMIN'
ON CONFLICT (username) DO NOTHING;

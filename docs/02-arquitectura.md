# Arquitectura propuesta

## Backend

Arquitectura por capas:

- Controller
- Service
- Repository
- Entity
- DTO
- Mapper
- Security
- Exception handling
- Audit

## Frontend

Arquitectura por módulos o features:

- auth
- dashboard
- products
- inventory
- sales
- purchases
- customers
- suppliers
- cash
- reports
- users
- shared
- core

## Base de datos

Base PostgreSQL con:

- Llaves primarias UUID
- Borrado lógico con `is_deleted`
- Fechas de auditoría `created_at`, `updated_at`, `deleted_at`
- Usuario creador/modificador/eliminador cuando aplique
- Bitácora centralizada en `audit_log`
- Movimientos de inventario separados de productos
- Movimientos de caja separados de ventas y compras

## Seguridad

- Login con JWT
- Password encriptado con BCrypt
- Roles: ADMIN, CAJERO, INVENTARIO, REPORTES
- Endpoints protegidos por rol
- Login público
- Rutas privadas bajo autenticación

# Sistema Tienda de Barrio Web

Sistema web para la gestión integral de una tienda de barrio.

## Módulos principales

- Productos
- Inventario
- Ventas / Punto de venta
- Compras
- Clientes
- Proveedores
- Caja y Finanzas
- Reportes
- Administración de usuarios y roles
- Bitácora / Auditoría

## Stack tecnológico

- Backend: Spring Boot
- Frontend: Angular
- UI: Tailwind CSS y Angular Material cuando aplique
- Base de datos: PostgreSQL
- Seguridad: JWT, roles y permisos
- IDE sugerido: Cursor

## Decisiones iniciales

- El sitio cargará inicialmente la pantalla de login.
- El backend usará borrado lógico mediante `is_deleted`.
- Las operaciones importantes deberán registrarse en bitácora.
- El punto de venta deberá permitir lectura por código de barras.
- Los métodos de pago iniciales serán: efectivo, transferencia y tarjeta.
- Caja y Finanzas será un módulo independiente, alimentado automáticamente por ventas, compras y movimientos manuales.

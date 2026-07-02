# Plan Backend Spring Boot

## Paquetes sugeridos

```text
com.tiendadebarrio
├── auth
├── users
├── roles
├── products
├── inventory
├── sales
├── purchases
├── customers
├── suppliers
├── cash
├── reports
├── audit
├── common
└── security
```

## Dependencias iniciales

- Spring Web
- Spring Data JPA
- Spring Security
- PostgreSQL Driver
- Validation
- Lombok
- jjwt
- Spring Boot Actuator
- Flyway o Liquibase

## Buenas prácticas

- Usar DTOs para requests y responses.
- No exponer entidades directamente.
- Validar datos con Bean Validation.
- Centralizar errores con `@ControllerAdvice`.
- Usar servicios transaccionales con `@Transactional`.
- Usar borrado lógico en lugar de eliminación física.
- Registrar bitácora para operaciones críticas.
- No guardar contraseñas en texto plano.
- Usar `BigDecimal` para montos.
- Controlar stock desde movimientos de inventario.
- No permitir ventas si no hay stock suficiente.

## Búsqueda de productos en POS

El backend debe permitir buscar productos por código de barras y por nombre.  
La búsqueda por código se usará cuando el lector de barras funcione correctamente.  
La búsqueda por nombre servirá como respaldo cuando el código no se lea, esté dañado o el producto no tenga código registrado.

La búsqueda por nombre debe filtrar únicamente productos activos y no eliminados lógicamente.

## Endpoints sugeridos iniciales

### Auth

- `POST /api/auth/login`
- `POST /api/auth/refresh` opcional
- `GET /api/auth/me`

### Productos

- `GET /api/products`
- `GET /api/products/{id}`
- `GET /api/products/barcode/{barcode}`
- `POST /api/products`
- `PUT /api/products/{id}`
- `DELETE /api/products/{id}` borrado lógico
- `GET /api/products/search?term={term}`

### Ventas

- `POST /api/sales`
- `GET /api/sales`
- `GET /api/sales/{id}`
- `POST /api/sales/{id}/cancel` opcional

### Compras

- `POST /api/purchases`
- `GET /api/purchases`
- `GET /api/purchases/{id}`

### Caja

- `POST /api/cash/sessions/open`
- `POST /api/cash/sessions/{id}/close`
- `GET /api/cash/sessions/current`
- `POST /api/cash/movements`
- `GET /api/cash/movements`

### Reportes

- `GET /api/reports/sales`
- `GET /api/reports/inventory`
- `GET /api/reports/cash`
- `GET /api/reports/top-products`

## Login inicial

La aplicación debe cargar primero la página de login. El backend debe permitir únicamente:

- `/api/auth/login`
- `/actuator/health`

El resto de endpoints requiere JWT.

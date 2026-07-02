# Prompts para Cursor

## 0. Leer planificación antes de generar código

## 1. Crear backend base

Crea un backend con Spring Boot para el proyecto `sistema-tienda-de-barrio-web`. Usa Java 21, Maven, PostgreSQL, Spring Web, Spring Data JPA, Spring Security, Validation y JWT. Organiza el código por módulos: auth, users, roles, products, inventory, sales, purchases, customers, suppliers, cash, reports, audit y common. Implementa arquitectura Controller, Service, Repository, Entity, DTO y Mapper. No expongas entidades directamente en los controladores.

## 2. Configurar seguridad JWT

Implementa seguridad JWT stateless. Permite acceso público únicamente a `/api/auth/login` y `/actuator/health`. Protege el resto de endpoints. Usa BCrypt para contraseñas. Crea roles ADMIN, CAJERO, INVENTARIO y REPORTES. Implementa `AuthController`, `AuthService`, `JwtService`, filtro JWT y endpoint `/api/auth/me`.

## 3. Implementar productos

Implementa el módulo de productos con CRUD completo usando borrado lógico. Campos principales: código de barras, SKU, nombre, descripción, categoría, unidad de medida, precio de venta, precio de compra, stock mínimo y estado activo. Agrega endpoint para buscar por código de barras: `GET /api/products/barcode/{barcode}`. Valida duplicados de código de barras y SKU.
Agrega también el endpoint `GET /api/products/search?term={term}` para buscar productos activos por coincidencia parcial de nombre. Esta búsqueda será usada en el POS cuando falle la lectura del código de barras o cuando el producto no tenga código registrado.

## 4. Implementar ventas / POS

Implementa el módulo de ventas. Una venta debe tener encabezado y detalle. Debe permitir método de pago EFECTIVO, TRANSFERENCIA o TARJETA. Al crear una venta, valida stock suficiente, descuenta inventario, registra movimientos de inventario tipo SALE y registra ingreso en caja. Usa transacciones para que todo se guarde o nada se guarde.

## 5. Implementar compras

Implementa compras con proveedor, detalle de productos, cantidades, costo unitario y total. Al confirmar compra, aumenta inventario, registra movimiento de inventario tipo PURCHASE y, si la compra está pagada, registra egreso en caja.

## 6. Implementar caja

Implementa caja con sesiones de apertura y cierre. Una sesión debe guardar monto inicial, monto esperado, monto contado, diferencia y estado ABIERTA/CERRADA. Los movimientos de caja pueden ser INCOME o EXPENSE y deben tener categoría, método de pago, monto y referencia opcional a venta o compra.

## 7. Crear frontend Angular

Crea un frontend Angular con Tailwind CSS y Angular Material. La ruta raíz debe redirigir a `/login`. Implementa AuthService, interceptor JWT, guard de autenticación y layout privado. Crea módulos o features: auth, dashboard, products, inventory, sales, purchases, customers, suppliers, cash, reports y users.

## 8. Crear pantalla POS

Crea una pantalla de punto de venta en Angular. Debe tener un campo principal para escanear código de barras o buscar productos por nombre. Si el valor ingresado coincide con un código de barras, consulta `/api/products/barcode/{barcode}`. Si no encuentra producto, permite buscar por nombre usando `/api/products/search?term={term}` y muestra resultados en autocomplete o lista seleccionable.

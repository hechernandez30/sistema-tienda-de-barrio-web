# Plan de Seguridad

## Autenticación

- Login con usuario o email y contraseña.
- Generación de JWT al autenticar correctamente.
- Token enviado por el frontend en header:

```http
Authorization: Bearer <token>
```

## Contraseñas

- Guardar contraseñas con BCrypt.
- Longitud mínima: 8 caracteres.
- Recomendado: mayúsculas, minúsculas y número.

## Roles iniciales

### ADMIN
Acceso completo.

### CAJERO
Acceso a ventas, clientes básicos y consulta de productos.

### INVENTARIO
Acceso a productos, inventario, compras y proveedores.

### REPORTES
Acceso a reportes.

## Reglas sugeridas

- Solo ADMIN administra usuarios y roles.
- CAJERO puede crear ventas, pero no modificar precios de productos.
- INVENTARIO puede crear productos, compras y ajustes de stock.
- REPORTES solo consulta reportes.
- Caja puede ser ADMIN y CAJERO, pero cierre con diferencias podría requerir ADMIN.

## Bitácora

Registrar:

- Inicio de sesión exitoso
- Intento fallido de login
- Creación, edición y eliminación lógica de productos
- Ventas creadas
- Ventas anuladas
- Compras creadas
- Ajustes de inventario
- Apertura y cierre de caja
- Movimientos manuales de caja
- Cambios de usuarios y roles

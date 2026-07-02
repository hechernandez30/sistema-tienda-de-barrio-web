# Visión funcional del proyecto

## Objetivo

Crear un sistema web que permita controlar las operaciones diarias de una tienda de barrio, principalmente productos, inventario, ventas, compras, caja, reportes y usuarios.

## Usuarios principales

### Administrador
Responsable de configurar usuarios, roles, productos, proveedores, reportes y supervisar caja.

### Cajera o dependiente
Responsable de vender productos desde el punto de venta, escanear códigos de barra, seleccionar método de pago y registrar la operación.

## Flujo principal de venta

1. La cajera inicia sesión.
2. Ingresa al módulo Punto de Venta.
3. Escanea el código de barras de un producto.
4. El sistema busca el producto activo por código de barras.
5. El sistema muestra nombre, precio, stock disponible y datos relevantes.
6. La cajera puede ingresar cantidad.
7. El sistema calcula subtotal, descuentos si aplican y total.
8. La cajera selecciona método de pago:
   - Efectivo por defecto
   - Transferencia
   - Tarjeta
9. El sistema confirma la venta.
10. El sistema descuenta inventario.
11. El sistema registra movimiento de inventario.
12. El sistema registra ingreso en caja.
13. El sistema registra bitácora.

## Caja y Finanzas

Caja debe ser un módulo independiente porque no solo registra ingresos por ventas, también puede registrar egresos, aperturas, cierres, ajustes y movimientos manuales.

Las ventas alimentan automáticamente caja como ingresos.  
Las compras alimentan caja como egresos cuando se registran como pagadas.  
Los movimientos manuales permiten registrar gastos como pago de servicios, transporte, retiros o ajustes.

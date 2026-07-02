# Capturas pendientes del Manual de Usuario

No fue posible tomar las capturas automáticamente, por lo que deben capturarse **manualmente**
desde el navegador (o el preview integrado de Cursor) con la aplicación en funcionamiento.

## Instrucciones generales

1. Iniciar el backend y el frontend (`npm start` en la carpeta `frontend`).
2. Abrir el sistema en el navegador e iniciar sesión con un usuario **ADMIN** (para ver todos los módulos).
3. Capturar cada pantalla indicada abajo.
4. Guardar cada imagen en `docs/manual-usuario/img/` con el **nombre exacto** de la tabla.
5. Formato recomendado: **PNG**, ancho aproximado **1280–1600 px**, sin datos sensibles reales.

> Sugerencia: tomar las capturas en **modo claro**, salvo la número 13, que debe ser en **modo oscuro**.

## Lista de capturas

| # | Archivo | Pantalla | Qué debe aparecer |
| - | ------- | -------- | ----------------- |
| 1 | `img/01-login.png` | Login | Tarjeta de inicio de sesión con campos de usuario y contraseña, botón **Iniciar sesión** y el botón de tema (sol/luna) en la esquina superior derecha. |
| 2 | `img/02-dashboard.png` | Dashboard | Panel principal con el menú lateral, las tarjetas de resumen y el encabezado (nombre de usuario, rol, botón de tema y **Salir**). |
| 3 | `img/03-productos-listado.png` | Productos – listado | Tabla de productos con el buscador arriba, columnas (código, nombre, precio, stock, estado) y los botones de acción por fila. |
| 4 | `img/04-productos-formulario.png` | Productos – formulario | Modal **Nuevo producto** con los campos visibles (código, nombre, precios, stock mínimo, stock inicial y casilla *Producto activo*). |
| 5 | `img/05-inventario.png` | Inventario | Pantalla con las pestañas **Movimientos** y **Stock bajo**, mostrando el historial de movimientos. |
| 6 | `img/06-proveedores.png` | Proveedores | Listado de proveedores con el buscador y las acciones por fila. |
| 7 | `img/07-compras.png` | Compras | Listado de compras (o el formulario de **Nueva compra** con proveedor y productos agregados). |
| 8 | `img/08-clientes.png` | Clientes | Listado de clientes con el buscador y las acciones por fila. |
| 9 | `img/09-caja.png` | Caja | Estado de la caja actual con el resumen (monto inicial, esperado, ingresos, egresos) y las pestañas de movimientos/sesiones. |
| 10 | `img/10-ventas-pos.png` | Ventas / POS | Punto de venta con el campo de escaneo/búsqueda, el carrito con productos, el selector de método de pago y el total. |
| 11 | `img/11-reportes.png` | Reportes | Filtros de fecha (Hoy / Este mes), tarjetas principales y alguna sección (ventas por método de pago o productos más vendidos). |
| 12 | `img/12-usuarios-roles.png` | Usuarios y Roles | Pestaña **Usuarios** con la tabla (usuario, nombre, email, rol, estado) y las acciones; opcionalmente mostrar la pestaña **Roles**. |
| 13 | `img/13-modo-oscuro.png` | Modo oscuro | Cualquier pantalla representativa (por ejemplo el Dashboard o el POS) con el **modo oscuro activado**, para evidenciar el contraste del tema oscuro. |
| 14 | `img/14-ventas-pos-nuevo-cliente.png` | Ventas / POS – nuevo cliente | Tarjeta **Cliente** del punto de venta mostrando el botón **+ Nuevo** (esquina superior derecha) y/o el formulario de **Nuevo cliente** abierto desde el POS. |
| 15 | `img/15-ventas-comprobante.png` | Ventas – comprobante | Comprobante de compra-venta en la **vista de impresión** del navegador (con el destino "Guardar como PDF" visible), o la tabla **Ventas del día** mostrando el ícono de impresora en las acciones. |

## Verificación final

- [ ] Las 15 imágenes están en `docs/manual-usuario/img/` con el nombre correcto.
- [ ] Las imágenes se ven en `manual-usuario.md` (abrir el archivo en un visor Markdown).
- [ ] No hay datos personales/reales sensibles en las capturas.

# Manual de Usuario

## Sistema Tienda de Barrio

> Versión del documento: 1.1
> Dirigido a: administradores, cajeros y encargados de tienda.

---

### Contenido

1. [Introducción](#1-introducción)
2. [Requisitos de uso](#2-requisitos-de-uso)
3. [Inicio de sesión](#3-inicio-de-sesión)
4. [Dashboard principal](#4-dashboard-principal)
5. [Productos](#5-productos)
6. [Inventario](#6-inventario)
7. [Proveedores](#7-proveedores)
8. [Compras](#8-compras)
9. [Clientes](#9-clientes)
10. [Caja](#10-caja)
11. [Ventas / Punto de Venta](#11-ventas--punto-de-venta)
12. [Reportes](#12-reportes)
13. [Usuarios y Roles](#13-usuarios-y-roles)
14. [Modo claro y modo oscuro](#14-modo-claro-y-modo-oscuro)
15. [Recomendaciones de uso](#15-recomendaciones-de-uso)
16. [Solución de problemas comunes](#16-solución-de-problemas-comunes)

---

### 1. Introducción

**Sistema Tienda de Barrio** es una aplicación web pensada para administrar de forma
ordenada la operación diaria de una tienda de barrio. Desde un solo lugar permite:

- Registrar y consultar **productos** y su **inventario**.
- Gestionar **proveedores** y **compras** de mercadería.
- Administrar **clientes** y registrar **ventas** desde un punto de venta ágil.
- Controlar el dinero mediante el módulo de **caja**.
- Consultar **reportes** de ventas, compras, inventario, caja y utilidad estimada.
- Administrar **usuarios y roles** con distintos niveles de acceso.

Cada usuario ve únicamente los módulos permitidos según su rol, lo que mantiene la
información protegida y la pantalla simple para cada persona.

---

### 2. Requisitos de uso

Para utilizar el sistema se necesita:

- Una **computadora** con un navegador web moderno (Google Chrome, Microsoft Edge o Firefox).
- **Conexión** al servidor donde está instalado el sistema (red local o internet, según la instalación).
- Un **usuario y contraseña** asignados por el administrador.
- De forma **opcional**, un **lector de código de barras** conectado por USB. El lector
  funciona como un teclado: no requiere configuración especial dentro del sistema.

> Recomendación: usar una pantalla de al menos 13", ya que el punto de venta y los
> reportes aprovechan mejor el espacio horizontal.

---

### 3. Inicio de sesión

La primera pantalla del sistema es el **inicio de sesión**.

Pasos:

1. Abrir el navegador e ingresar la dirección del sistema proporcionada por el administrador.
2. Escribir el **usuario** en el primer campo.
3. Escribir la **contraseña** en el segundo campo. Puede pulsarse el ícono del ojo para
   mostrar u ocultar la contraseña.
4. Pulsar el botón **Iniciar sesión**.

Si las credenciales son correctas, el sistema abre el **Dashboard**.

**Si las credenciales son incorrectas** o el usuario está inactivo, aparece un mensaje de
error en rojo debajo del título. En ese caso se debe verificar el usuario y la contraseña,
respetando mayúsculas y minúsculas. Si el problema continúa, contactar al administrador.

**Cambiar el tema desde el login:** en la esquina superior derecha hay un botón con un ícono
de sol/luna que permite alternar entre **modo claro** y **modo oscuro** antes de ingresar.

![Login](img/01-login.png)

---

### 4. Dashboard principal

Tras iniciar sesión se muestra el **Dashboard**, el panel principal del sistema.

Elementos de la pantalla:

- **Menú lateral (izquierda):** lista de módulos disponibles. Solo aparecen los módulos que
  el rol del usuario tiene permitido. En pantallas pequeñas el menú se abre con el botón de
  menú (☰) del encabezado.
- **Tarjetas de resumen:** muestran información general de la operación para dar un vistazo rápido.
- **Encabezado superior (header):** a la derecha muestra el **nombre del usuario**, su **rol**,
  el botón para **cambiar de tema** (sol/luna) y el botón **Salir** para cerrar sesión.
- **Navegación:** para entrar a un módulo basta con hacer clic en su nombre en el menú lateral.

**Cerrar sesión:** pulsar el botón **Salir** en la esquina superior derecha. Es recomendable
cerrar sesión al terminar la jornada o al alejarse de la computadora.

![Dashboard](img/02-dashboard.png)

---

### 5. Productos

El módulo **Productos** permite administrar el catálogo de la tienda.
*(Disponible para los roles ADMIN e INVENTARIO.)*

**Consultar y buscar productos**

- Al entrar se muestra la lista de productos con su código de barras, nombre, precio de venta,
  stock actual y estado.
- En la parte superior hay un **buscador**: al escribir se filtran los productos por **nombre,
  SKU o código de barras**.

**Crear un producto**

1. Pulsar **Nuevo producto**.
2. Completar los datos: código de barras, SKU (opcional), nombre, descripción (opcional),
   **precio de compra**, **precio de venta**, **stock mínimo** y **stock inicial**.
3. Dejar marcada la casilla **Producto activo** si estará disponible para la venta.
4. Pulsar **Guardar**.

> Los campos de precio y stock solo aceptan números (no permiten letras ni la tecla "e").
> Al hacer clic en el campo, el `0` se selecciona para que se reemplace al escribir.

**Stock inicial y movimiento automático de inventario**

Si al crear el producto se ingresa un **stock inicial mayor a 0**, el sistema genera
automáticamente un **movimiento de inventario de entrada** con la nota "Inventario inicial".
Así el inventario queda con trazabilidad desde el primer día.

**Editar un producto**

- Pulsar el ícono de **editar** en la fila del producto. El **stock actual no se modifica**
  desde aquí: los cambios de stock se realizan en el módulo de **Inventario**.

**Ver detalle / Eliminar**

- El ícono de **ver** muestra el detalle completo del producto.
- El ícono de **eliminar** realiza un **borrado lógico**: el producto deja de estar disponible,
  pero se conserva su historial.

**Estado y stock bajo**

- El **estado** se muestra con una etiqueta: *Activo* o *Inactivo*.
- Cuando el stock actual llega al **stock mínimo** o menos, el producto se resalta como
  **stock bajo** para avisar que conviene reabastecer.

![Listado de productos](img/03-productos-listado.png)

![Formulario de producto](img/04-productos-formulario.png)

---

### 6. Inventario

El módulo **Inventario** controla las existencias y su historial.
*(Disponible para los roles ADMIN e INVENTARIO.)*

La pantalla tiene dos pestañas:

- **Movimientos:** historial de todas las entradas y salidas de inventario.
- **Stock bajo:** lista de productos que están en su nivel mínimo o por debajo.

**Cómo se genera el historial**

El inventario se actualiza automáticamente por las operaciones del sistema:

- **Compras confirmadas** → generan entradas.
- **Ventas registradas** → generan salidas.
- **Ajustes manuales** → entradas o salidas realizadas por el encargado.

**Registrar un ajuste de inventario**

1. Pulsar el botón para **registrar ajuste**.
2. Elegir el tipo:
   - **Ajuste de entrada** (aumenta el stock): por ejemplo, mercadería encontrada o corrección.
   - **Ajuste de salida** (disminuye el stock): por ejemplo, producto dañado, vencido o merma.
3. Buscar y seleccionar el producto.
4. Indicar la **cantidad** y una **nota** que explique el motivo.
5. Guardar.

> Los ajustes manuales deben usarse solo cuando sea necesario, ya que modifican el stock
> sin pasar por una compra o venta.

![Inventario](img/05-inventario.png)

---

### 7. Proveedores

El módulo **Proveedores** administra a quienes surten mercadería a la tienda.
*(Disponible para los roles ADMIN e INVENTARIO.)*

Permite:

- **Consultar** la lista de proveedores con su estado.
- **Buscar** un proveedor por nombre u otros datos.
- **Crear** un proveedor con sus datos de contacto.
- **Editar** los datos de un proveedor existente.
- **Ver** el detalle de un proveedor.
- **Eliminar** un proveedor (borrado lógico, se conserva el historial).

**Relación con compras:** al registrar una compra se debe seleccionar un proveedor, por lo que
conviene tener los proveedores creados antes de comprar.

![Proveedores](img/06-proveedores.png)

---

### 8. Compras

El módulo **Compras** registra el ingreso de mercadería adquirida a proveedores.
*(Disponible para los roles ADMIN e INVENTARIO.)*

**Crear una compra**

1. Pulsar **Nueva compra**.
2. Seleccionar el **proveedor**.
3. Agregar los **productos**: buscar cada producto, indicar la **cantidad** y el **costo unitario**.
4. El sistema calcula automáticamente los **totales**.
5. Pulsar **Guardar** para registrar la compra.

**Confirmar una compra**

- Una compra recién creada puede quedar pendiente. Al **confirmarla**, el sistema **aumenta el
  inventario** de cada producto incluido, generando los movimientos de entrada correspondientes.

**Cancelar una compra**

- Una compra puede **cancelarse** si fue registrada por error. El sistema mostrará el detalle y
  pedirá confirmación.

**Ver detalle**

- Desde la lista se puede abrir el **detalle** de cada compra para revisar proveedor, productos,
  cantidades, costos y estado.

> Importante: mientras una compra no se **confirme**, el inventario **no** se actualiza.

![Compras](img/07-compras.png)

---

### 9. Clientes

El módulo **Clientes** administra la información de los clientes de la tienda.
*(Disponible para los roles ADMIN y CAJERO.)*

Permite:

- **Consultar** la lista de clientes.
- **Buscar** un cliente por nombre, NIT o teléfono.
- **Crear** un cliente. El campo **teléfono** admite hasta **8 dígitos** (solo números).
- **Editar** los datos de un cliente.
- **Ver** el detalle de un cliente.
- **Eliminar** un cliente (borrado lógico).

**Uso en ventas:** seleccionar un cliente en la venta es **opcional**. Si no se selecciona
ninguno, la venta se registra como **Consumidor final**.

![Clientes](img/08-clientes.png)

---

### 10. Caja

El módulo **Caja** controla el dinero durante la jornada.
*(Disponible para los roles ADMIN, CAJERO y REPORTES; las operaciones de abrir/cerrar y
registrar movimientos corresponden a ADMIN y CAJERO.)*

**Abrir caja**

1. Pulsar **Abrir caja**.
2. Ingresar el **monto inicial** (el efectivo con el que se inicia el día).
3. Confirmar.

> Es obligatorio **abrir la caja antes de poder registrar ventas** en el punto de venta.

**Registrar movimientos manuales**

- **Ingreso manual:** dinero que entra por un motivo distinto a una venta.
- **Egreso manual:** dinero que sale (por ejemplo, pago de un servicio o un retiro).

En cada movimiento se indica la categoría, el método de pago, el monto y una descripción.

**Consultar movimientos y sesiones**

- La pestaña de **movimientos** muestra ingresos y egresos de la caja abierta.
- La pestaña de **sesiones** muestra el historial de cajas abiertas y cerradas, con su detalle.

**Cerrar caja**

1. Pulsar **Cerrar caja**.
2. El sistema muestra el **monto esperado** (calculado a partir del monto inicial más los
   ingresos, menos los egresos).
3. Ingresar el **monto contado** (el efectivo real en caja).
4. El sistema calcula la **diferencia**:
   - **Cero:** la caja cuadra.
   - **Positiva:** hay un **sobrante**.
   - **Negativa:** hay un **faltante**.
5. Confirmar el cierre.

![Caja](img/09-caja.png)

---

### 11. Ventas / Punto de Venta

El módulo **Ventas / Punto de Venta (POS)** permite cobrar de forma rápida.
*(Disponible para los roles ADMIN y CAJERO.)*

**Requisito: caja abierta**

- Si **no hay caja abierta**, el sistema muestra un aviso y un botón para ir a **Caja**.
  No es posible registrar ventas hasta abrir la caja.

**Agregar productos**

1. Colocar el cursor en el campo **"Escanear código o buscar producto..."** (siempre queda enfocado).
2. Opciones para agregar:
   - **Escanear** el código de barras con el lector y pulsar Enter.
   - **Escribir el código** y pulsar Enter.
   - **Escribir parte del nombre**: aparecen coincidencias para **seleccionar** el producto.
3. Al agregar un producto ya presente en el carrito, su cantidad **aumenta en 1**.

**Editar el carrito**

- Cambiar la **cantidad** con los botones **+ / −** o escribiéndola directamente.
- El sistema **no permite vender más que el stock disponible** y avisa si se excede.
- Eliminar una línea con el ícono de **eliminar**.

**Cliente y método de pago**

- Seleccionar un **cliente** es opcional; sin selección, la venta es para **Consumidor final**.
- Elegir el **método de pago**: **Efectivo** (por defecto), **Transferencia** o **Tarjeta**.

**Agregar un cliente nuevo desde el punto de venta**

Si el cliente todavía no existe, no es necesario salir del punto de venta:

1. En la tarjeta **Cliente**, pulsar el botón **+ Nuevo** (esquina superior derecha de la tarjeta).
2. Se abre el **mismo formulario de "Nuevo cliente"** del módulo de Clientes.
3. Completar los datos y pulsar **Guardar**.
4. Al guardarse, el cliente queda **seleccionado automáticamente** en la tarjeta Cliente, listo para la venta.

> El cliente creado también queda disponible de forma permanente en el módulo **Clientes**.

![Agregar cliente desde el POS](img/14-ventas-pos-nuevo-cliente.png)

**Registrar la venta**

1. Verificar el carrito y el **total a pagar**.
2. Pulsar **Registrar venta**.
3. Si la venta es correcta, el sistema muestra un mensaje de éxito, **limpia el carrito**,
   vuelve a **Consumidor final** y a **Efectivo**, y deja listo el campo para la siguiente venta.

**Ventas del día y anulación**

- En la parte inferior se muestran las **ventas del día** con su número, hora, cliente,
  método de pago, total y estado.
- Se puede **ver el detalle** de cada venta.
- **Anular una venta** solo está disponible para el rol **ADMIN**. Al anular, el sistema
  **devuelve el stock** y **registra la anulación en caja**.

**Imprimir o guardar el comprobante (PDF)**

Cada venta puede entregarse al cliente como **comprobante de compra-venta**:

- Desde la tabla **Ventas del día**, pulsar el ícono de **impresora** en la fila de la venta,
  **sin necesidad de abrir el detalle**.
- O bien, abrir **Ver detalle** de la venta y pulsar **Imprimir / Guardar PDF**.

En ambos casos se abre la **vista de impresión** del navegador. Desde ahí se puede:

- **Imprimir** directamente en una impresora, o
- **Guardar como PDF** eligiendo esa opción como impresora de destino.

El comprobante incluye el número de venta, fecha y hora, cajero, cliente (o **Consumidor final**),
método de pago, el detalle de productos con cantidades y precios, y los totales. Las ventas
**anuladas** se marcan claramente como tal en el comprobante.

> Si no se abre la ventana de impresión, revisar que el navegador **no esté bloqueando las
> ventanas emergentes** para el sistema.

![Punto de venta](img/10-ventas-pos.png)

![Comprobante de venta](img/15-ventas-comprobante.png)

---

### 12. Reportes

El módulo **Reportes** ofrece indicadores de la operación.
*(Disponible para los roles ADMIN y REPORTES.)*

**Filtros de fecha**

- Elegir **Fecha desde** y **Fecha hasta** y pulsar **Aplicar filtros**.
- Botones rápidos: **Hoy** (día actual) y **Este mes** (del día 1 al día actual).

**Indicadores disponibles**

- **Tarjetas principales:** ventas totales, cantidad de ventas, ticket promedio, utilidad
  estimada, ingresos netos de caja y productos con stock bajo.
- **Ventas por método de pago:** cuánto se vendió en efectivo, transferencia y tarjeta.
- **Ventas diarias:** total de ventas por día dentro del rango.
- **Productos más vendidos:** ranking por cantidad e importe.
- **Stock bajo:** productos que requieren reabastecimiento.
- **Resumen de inventario:** productos, activos, stock bajo y valorización.
- **Compras:** resumen y compras por proveedor.
- **Caja:** ingresos por método, egresos y neto.
- **Utilidad estimada:** ventas, costo estimado y utilidad bruta estimada.

**Interpretación básica**

- El **ticket promedio** indica cuánto compra en promedio cada cliente.
- La **utilidad estimada** se calcula con el **costo actual** de los productos, por lo que es
  una **estimación** y puede variar si los costos cambian.
- Un **stock bajo** alto sugiere planificar compras.

![Reportes](img/11-reportes.png)

---

### 13. Usuarios y Roles

El módulo **Usuarios y Roles** administra los accesos al sistema.
*(Disponible únicamente para el rol ADMIN.)*

La pantalla tiene dos pestañas: **Usuarios** y **Roles**.

**Usuarios**

- **Listar y buscar** usuarios por usuario, nombre, email o rol.
- **Crear usuario:** completar usuario, email (opcional), contraseña y su confirmación,
  nombre, apellido, rol y estado activo. La contraseña debe tener **mínimo 8 caracteres, con
  al menos una mayúscula, una minúscula y un número**.
- **Editar usuario:** el **nombre de usuario no se puede cambiar**; sí el email, nombre,
  apellido, rol y estado.
- **Cambiar contraseña:** asignar una nueva contraseña (con las mismas reglas de seguridad).
- **Activar / Desactivar:** un usuario inactivo **no puede iniciar sesión**.
- **Eliminar:** borrado lógico; se conserva el historial para auditoría.

> Por seguridad, el usuario con el que se ha iniciado sesión aparece marcado como **"Tú"** y no
> puede desactivarse ni eliminarse a sí mismo. El sistema también protege al último administrador activo.

**Roles disponibles**

| Rol | Alcance |
| --- | --- |
| **ADMIN** | Acceso completo al sistema, incluida la administración de usuarios y roles. |
| **CAJERO** | Ventas, clientes y caja. |
| **INVENTARIO** | Productos, inventario, compras y proveedores. |
| **REPORTES** | Reportes administrativos (y consulta de caja). |

En la pestaña **Roles** se puede consultar cada rol y **ver su detalle** con la explicación
de lo que permite.

![Usuarios y roles](img/12-usuarios-roles.png)

---

### 14. Modo claro y modo oscuro

El sistema puede mostrarse en **modo claro** o **modo oscuro**.

- Para cambiar el tema, pulsar el botón con el ícono de **sol/luna** ubicado en:
  - la **esquina superior derecha** de la pantalla de **login**, o
  - el **encabezado** del sistema, una vez que se ha iniciado sesión.
- La **preferencia se guarda automáticamente** en el navegador.
- El tema elegido **se mantiene al recargar** la página o al volver a ingresar en esa computadora.

![Modo oscuro](img/13-modo-oscuro.png)

---

### 15. Recomendaciones de uso

- **Abrir la caja antes de vender:** sin caja abierta no se pueden registrar ventas.
- **Crear los productos primero:** antes de registrar compras y ventas conviene tener el
  catálogo de productos cargado.
- **Usar ajustes de inventario solo cuando sea necesario:** para correcciones, mermas o
  productos dañados; el flujo normal es por compras y ventas.
- **Confirmar las compras:** una compra solo actualiza el inventario cuando se confirma.
- **Revisar los reportes al cierre del día:** ayudan a controlar ventas, caja y stock.
- **Cerrar la caja al final de la jornada** y comparar el monto esperado con el contado.
- **Cerrar sesión al terminar** o al alejarse de la computadora.
- **No compartir las contraseñas** y usar cada quien su propio usuario.

---

### 16. Solución de problemas comunes

**No puedo iniciar sesión.**
Verificar el usuario y la contraseña (respetando mayúsculas y minúsculas). Si el usuario está
**inactivo**, no podrá ingresar; contactar al administrador para reactivarlo.

**No aparece un producto al escanear.**
Puede que el producto no exista o que el código sea distinto. Escribir parte del **nombre** en
el campo de búsqueda para localizarlo. Si aún no existe, crearlo en el módulo **Productos**.

**No puedo vender porque no hay caja abierta.**
Ir al módulo **Caja**, pulsar **Abrir caja** e ingresar el monto inicial. Luego volver al punto
de venta.

**No puedo vender porque no hay stock.**
El sistema no permite vender más unidades que las disponibles. Verificar el stock del producto y,
si corresponde, registrar una **compra** (y confirmarla) o un **ajuste de entrada** en Inventario.

**Una compra no actualizó el inventario.**
Probablemente la compra quedó **sin confirmar**. Abrir la compra y **confirmarla**; en ese momento
se actualiza el inventario.

**No veo ciertos módulos en el menú.**
El menú muestra solo los módulos permitidos según el **rol** del usuario. Si se necesita acceso a
otro módulo, solicitarlo al administrador.

**El sistema se ve oscuro o claro y quiero cambiarlo.**
Pulsar el botón de **sol/luna** en el encabezado (o en el login) para alternar el tema. La
preferencia se guarda automáticamente.

---

*Fin del manual.*

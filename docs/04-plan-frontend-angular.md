# Plan Frontend Angular

## Estructura sugerida

```text
src/app
├── core
│   ├── interceptors
│   ├── guards
│   ├── services
│   └── models
├── shared
│   ├── components
│   ├── pipes
│   └── utils
├── features
│   ├── auth
│   ├── dashboard
│   ├── products
│   ├── inventory
│   ├── sales
│   ├── purchases
│   ├── customers
│   ├── suppliers
│   ├── cash
│   ├── reports
│   └── users
└── layout
```

## Librerías sugeridas

- Angular Router
- Reactive Forms
- HttpClient
- Tailwind CSS
- Angular Material para:
  - tablas
  - diálogos
  - snackbars
  - inputs
  - selects
  - datepickers
  - botones

## Rutas iniciales

```ts
/login
/app/dashboard
/app/products
/app/inventory
/app/sales/pos
/app/purchases
/app/customers
/app/suppliers
/app/cash
/app/reports
/app/users
```

## Comportamiento inicial

- Ruta raíz `/` redirige a `/login`.
- Si el usuario no tiene token válido, cualquier ruta privada redirige a `/login`.
- Si el usuario ya tiene token válido y entra a `/login`, puede redirigir a `/app/dashboard`.

## Punto de venta

El campo principal del POS debe permitir escanear código de barras o buscar productos por nombre.

Comportamiento esperado:

1. El campo principal debe estar enfocado automáticamente.
2. Si se escanea un código de barras, el sistema debe buscar el producto por código.
3. Si el producto no se encuentra, debe mostrar un mensaje claro.
4. Como respaldo, la cajera puede buscar el producto por nombre.
5. La búsqueda por nombre debe mostrar coincidencias en formato autocomplete o lista seleccionable.
6. Al seleccionar un producto, se agrega al carrito de venta.
7. Si el producto ya existe en el carrito, se aumenta la cantidad.
8. La cantidad puede modificarse manualmente.
9. El método de pago por defecto será efectivo.
10. La cajera podrá cambiar el método de pago a transferencia o tarjeta.

# Manual Técnico

## Sistema Tienda de Barrio (Variedades Hernández)

> Versión del documento: 1.0
> Dirigido a: personal técnico, desarrolladores y encargados de despliegue y mantenimiento.

---

### Contenido

1. [Introducción](#1-introducción)
2. [Arquitectura general](#2-arquitectura-general)
3. [Stack tecnológico](#3-stack-tecnológico)
4. [Estructura del proyecto](#4-estructura-del-proyecto)
5. [Backend (Spring Boot)](#5-backend-spring-boot)
6. [Modelo de datos](#6-modelo-de-datos)
7. [API REST](#7-api-rest)
8. [Frontend (Angular)](#8-frontend-angular)
9. [Seguridad](#9-seguridad)
10. [Configuración por variables de entorno](#10-configuración-por-variables-de-entorno)
11. [Despliegue con Docker](#11-despliegue-con-docker)
12. [Base de datos: inicialización, respaldos y migración](#12-base-de-datos-inicialización-respaldos-y-migración)
13. [Ejecución en desarrollo](#13-ejecución-en-desarrollo)
14. [Operación y mantenimiento](#14-operación-y-mantenimiento)
15. [Consideraciones a futuro](#15-consideraciones-a-futuro)

---

### 1. Introducción

**Sistema Tienda de Barrio** es una aplicación web para administrar la operación diaria de
una tienda: productos, inventario, proveedores, compras, clientes, caja, ventas (punto de
venta), reportes y administración de usuarios.

Este manual describe la **arquitectura técnica**, la **estructura del código**, el **modelo
de datos**, la **API**, la **seguridad** y los procedimientos de **despliegue y
mantenimiento**. Para el uso funcional del sistema, consultar el *Manual de Usuario*.

El sistema está compuesto por tres piezas que se ejecutan como contenedores Docker:

- Una **base de datos** PostgreSQL.
- Un **backend** REST en Spring Boot.
- Un **frontend** Angular servido por Nginx, que además hace de proxy hacia el backend.

---

### 2. Arquitectura general

La arquitectura es de tres capas desacopladas que se comunican por HTTP:

- **Navegador (cliente):** carga la aplicación Angular servida por Nginx.
- **Frontend / Nginx:** sirve los archivos estáticos de Angular y actúa como *reverse
  proxy*: cualquier petición a `/api` se reenvía al backend. Esto hace que frontend y API
  compartan el mismo origen (puerto 80), evitando problemas de CORS.
- **Backend / Spring Boot:** expone la API REST, aplica reglas de negocio, seguridad y
  auditoría, y persiste en PostgreSQL.
- **Base de datos / PostgreSQL:** almacena la información en un volumen persistente.

Flujo de una petición típica:

1. El navegador pide `http://SERVIDOR/` → Nginx devuelve la app Angular.
2. Angular llama a `http://SERVIDOR/api/...` → Nginx lo reenvía a `http://backend:8080/api/...`.
3. El backend valida el token JWT, ejecuta la lógica y consulta PostgreSQL.
4. La respuesta regresa al navegador.

![Diagrama de arquitectura](img/01-arquitectura.png)

---

### 3. Stack tecnológico

**Backend**

- Java 21
- Spring Boot 3.4.4 (Web, Data JPA, Security, Validation, Actuator)
- Maven (gestión de dependencias y build)
- PostgreSQL Driver
- JJWT 0.12.6 (generación y validación de tokens JWT)
- BCrypt (hash de contraseñas)
- Lombok (reducción de código repetitivo)

**Frontend**

- Angular 18 (componentes *standalone*)
- TypeScript
- Tailwind CSS 3 (estilos utilitarios, modo oscuro por clase)
- Angular Material (principalmente diálogos)
- RxJS y *Signals* (estado reactivo)

**Infraestructura**

- Docker y Docker Compose
- PostgreSQL 16 (contenedor)
- Nginx (servir el frontend y *proxy* de la API)

---

### 4. Estructura del proyecto

```text
sistema-tienda-de-barrio-web/
├── backend/                 # API Spring Boot
│   ├── src/main/java/com/tiendadebarrio/
│   ├── src/main/resources/application.yml
│   ├── Dockerfile
│   └── .dockerignore
├── frontend/                # Aplicación Angular
│   ├── src/app/
│   ├── src/environments/
│   ├── Dockerfile
│   ├── nginx.conf
│   └── .dockerignore
├── database/
│   └── init.sql             # Esquema y datos base (fuente original)
├── docker/
│   ├── postgres/init/01-init.sql   # Copia usada por Docker en el primer arranque
│   └── backups/             # Respaldos generados (ignorados por Git)
├── scripts/
│   ├── backup-db.sh
│   └── restore-db.sh
├── docs/
│   ├── manual-usuario/
│   └── manual-tecnico/
├── docker-compose.yml
├── .env.example
└── README-Docker.md
```

---

### 5. Backend (Spring Boot)

**Organización modular.** El código se organiza por dominios funcionales dentro del paquete
`com.tiendadebarrio`. Cada módulo agrupa sus propias capas: `controller`, `service`, `dto`,
`entity`, `mapper` y `repository`.

Módulos principales:

- `auth` — autenticación (login) y perfil del usuario actual.
- `security` — configuración de Spring Security, filtro JWT, utilidades de seguridad.
- `users` y `roles` — administración de usuarios y roles.
- `products` — productos, además de **categorías** y **unidades de medida**.
- `inventory` — movimientos y control de existencias.
- `suppliers` — proveedores.
- `purchases` — compras y sus ítems.
- `customers` — clientes.
- `cash` — sesiones y movimientos de caja.
- `sales` — ventas (punto de venta).
- `reports` — reportes e indicadores.
- `audit` — bitácora de acciones.
- `common` — utilidades transversales (entidad auditable, manejo de errores, seeder).

**Capas por módulo.**

- **Controller:** expone los *endpoints* REST y aplica autorización por rol con
  `@PreAuthorize`.
- **Service:** contiene la lógica de negocio y las transacciones (`@Transactional`).
- **DTO:** objetos de entrada/salida; nunca se exponen las entidades directamente.
- **Mapper:** convierte entre entidades y DTOs.
- **Repository:** acceso a datos con Spring Data JPA.

**Entidad auditable y borrado lógico.** La clase base `AuditableEntity` aporta los campos
`is_deleted`, `created_at`, `updated_at` y `deleted_at`. Las eliminaciones son **lógicas**
(*soft delete*): el registro se marca como borrado y se conserva para trazabilidad.

**Auditoría.** El módulo `audit` registra las acciones relevantes (crear, actualizar,
eliminar) con el usuario, el módulo, la entidad y los valores anterior/nuevo.

**Manejo de errores.** Existe un manejador global de excepciones que traduce los errores de
negocio a respuestas HTTP consistentes (código, mensaje y estado).

**Seeder de datos.** Al arrancar, `DataSeeder` garantiza que existan los roles base y el
usuario `admin`. Si el usuario `admin` tiene el hash de marcador que trae el `init.sql`, lo
**reemplaza por un hash BCrypt real** de la contraseña por defecto.

---

### 6. Modelo de datos

La base de datos usa **PostgreSQL** con las siguientes características:

- **Claves primarias UUID** (extensión `uuid-ossp`).
- **Tipos ENUM nativos**: `payment_method`, `inventory_movement_type`,
  `cash_movement_type`, `cash_session_status`, `sale_status`, `purchase_status`.
- **Numeración correlativa** en ventas y compras con `BIGSERIAL` (`sale_number`,
  `purchase_number`).
- **Montos** con `NUMERIC(12,2)` y **cantidades** con `NUMERIC(12,3)` para precisión.
- **Restricciones** (`CHECK`) e **índices únicos parciales** (por ejemplo, código de barras
  único solo entre productos no eliminados).

Tablas principales:

| Tabla | Descripción |
| --- | --- |
| `roles`, `permissions`, `role_permissions` | Roles y permisos del sistema. |
| `app_users` | Usuarios, con referencia a su rol y hash de contraseña. |
| `product_categories`, `unit_measures` | Catálogos de apoyo para productos. |
| `products` | Catálogo de productos (precios, stock, categoría, unidad). |
| `customers`, `suppliers` | Clientes y proveedores. |
| `cash_sessions`, `cash_movements` | Sesiones de caja y sus movimientos. |
| `sales`, `sale_items` | Ventas y su detalle. |
| `purchases`, `purchase_items` | Compras y su detalle. |
| `inventory_movements` | Historial de entradas/salidas de inventario. |
| `audit_log` | Bitácora de acciones del sistema. |

**Reglas destacadas del esquema:**

- Solo puede existir **una sesión de caja abierta** a la vez (índice único parcial sobre
  `status = 'OPEN'`).
- El **stock** no puede ser negativo (restricciones `CHECK`).
- Las ventas y compras conservan datos "congelados" en su detalle (por ejemplo, nombre y
  código del producto al momento de la venta).

![Modelo de datos](img/02-modelo-datos.png)

---

### 7. API REST

Todos los *endpoints* se publican bajo el prefijo `/api`. La autenticación es por **token
JWT** en el encabezado `Authorization: Bearer <token>`, salvo el login.

Recursos por módulo:

| Prefijo | Módulo | Notas |
| --- | --- | --- |
| `/api/auth` | Autenticación | `POST /login`, `GET /me`. Login es público. |
| `/api/users` | Usuarios | CRUD, cambio de contraseña, activar/desactivar. Solo ADMIN. |
| `/api/roles` | Roles | Listado y detalle de roles. |
| `/api/products` | Productos | CRUD, búsqueda y consulta por código de barras. |
| `/api/categories` | Categorías | Listar y crear. |
| `/api/unit-measures` | Unidades de medida | Listar y crear. |
| `/api/inventory` | Inventario | Movimientos, stock bajo y ajustes. |
| `/api/suppliers` | Proveedores | CRUD y búsqueda. |
| `/api/purchases` | Compras | Crear, confirmar, cancelar y detalle. |
| `/api/customers` | Clientes | CRUD y búsqueda. |
| `/api/cash` | Caja | Abrir/cerrar sesión, movimientos e historial. |
| `/api/sales` | Ventas | Registrar, anular, listar y detalle. |
| `/api/reports` | Reportes | Indicadores por rango de fechas. |

**Autenticación (ejemplo).**

`POST /api/auth/login`

```json
{
  "username": "admin",
  "password": "Admin123"
}
```

La respuesta incluye el **token JWT** y los datos del usuario (nombre y rol). El frontend
guarda el token y lo envía en cada petición mediante un *interceptor*.

**Health check.** `GET /actuator/health` expone el estado del backend (usado también por
Nginx y para monitoreo).

---

### 8. Frontend (Angular)

**Arquitectura.** Aplicación Angular 18 con **componentes standalone** (sin NgModules). El
código se organiza en:

- `core/` — modelos, servicios (HTTP), *guards* e *interceptors*.
- `shared/` — componentes reutilizables (diálogos de confirmación, creación rápida, etc.).
- `features/` — pantallas por módulo (productos, inventario, ventas, etc.).
- `layout/` — estructura pública (login) y privada (menú lateral + encabezado).

**Ruteo y acceso.** Las rutas usan **carga diferida** (*lazy loading*) y **guards
funcionales**:

- `authGuard` — exige sesión iniciada.
- `roleGuard` — restringe rutas por rol, alineado con `@PreAuthorize` del backend.
- `publicGuard` — evita entrar al login si ya hay sesión.

**Comunicación con la API.** Un **interceptor HTTP** agrega el token JWT a las peticiones y
gestiona respuestas de error/expiración. La URL base de la API se resuelve por entorno:

- Desarrollo (`environment.ts`): apunta al backend por el *host* actual.
- Producción (`environment.prod.ts`): usa la ruta relativa `/api` (proxy de Nginx).

**Estado y UX.** Se usan *Signals* para estado local reactivo, operadores RxJS
(`debounceTime`, `switchMap`, etc.) para búsquedas, y un diseño unificado con Tailwind que
incluye **modo claro/oscuro** persistido en el navegador.

---

### 9. Seguridad

- **Autenticación JWT.** El login valida credenciales y emite un token firmado (JJWT). El
  token tiene una expiración configurable (`JWT_EXPIRATION_MINUTES`).
- **Contraseñas con BCrypt.** Nunca se almacenan en texto plano.
- **Autorización por rol.** Se aplica en el backend con `@PreAuthorize` a nivel de método y
  se refleja en el frontend con `roleGuard` y la visibilidad del menú.
- **Roles del sistema:**
  - `ADMIN` — acceso completo, incluida la administración de usuarios y roles.
  - `CAJERO` — ventas, clientes y caja.
  - `INVENTARIO` — productos, inventario, compras y proveedores.
  - `REPORTES` — reportes administrativos (y consulta de caja).
- **Protecciones de negocio.** Un usuario no puede desactivarse/eliminarse a sí mismo y se
  protege al último administrador activo.
- **CORS.** Configurado en el backend. En el despliegue con Docker, frontend y API comparten
  origen (Nginx), por lo que no se requieren reglas CORS especiales.
- **Sesiones sin estado.** El backend es *stateless*: cada petición se autentica con el token.

---

### 10. Configuración por variables de entorno

El backend lee su configuración desde variables de entorno (con valores por defecto para
desarrollo). Las principales:

| Variable | Descripción |
| --- | --- |
| `SPRING_DATASOURCE_URL` | URL JDBC de PostgreSQL. En Docker: `jdbc:postgresql://postgres:5432/tienda_barrio_db`. |
| `SPRING_DATASOURCE_USERNAME` | Usuario de la base de datos. |
| `SPRING_DATASOURCE_PASSWORD` | Contraseña de la base de datos. |
| `JWT_SECRET` | Clave secreta para firmar los tokens JWT. |
| `JWT_EXPIRATION_MINUTES` | Minutos de validez del token. |
| `SERVER_PORT` | Puerto del backend (8080). |

Para Docker Compose, estas variables se definen en el archivo `.env` (a partir de
`.env.example`). El archivo `.env` real **no se versiona**.

> Importante: `spring.jpa.hibernate.ddl-auto` está en **`validate`**. El esquema lo crea el
> script `init.sql`, no Hibernate. Nunca usar `create` ni `create-drop`.

---

### 11. Despliegue con Docker

El archivo `docker-compose.yml` define tres servicios:

| Servicio | Imagen | Puerto host | Rol |
| --- | --- | --- | --- |
| `postgres` | `postgres:16` | 5432 | Base de datos con volumen persistente y *healthcheck*. |
| `backend` | build `backend/Dockerfile` | 8080 | API Spring Boot. Depende de `postgres` saludable. |
| `frontend` | build `frontend/Dockerfile` | 80 | Angular + Nginx (entrada principal y proxy `/api`). |

**Dockerfiles.**

- `backend/Dockerfile`: *build* multi-etapa con Java 21 (Maven compila el JAR; la imagen
  final usa `eclipse-temurin:21-jre`).
- `frontend/Dockerfile`: *build* multi-etapa con Node 20 (compila Angular en producción) y
  luego sirve los estáticos con `nginx:alpine`.

**Levantar el sistema:**

```bash
cp .env.example .env
docker compose up -d --build
```

Acceso: `http://localhost` (o `http://IP_DEL_SERVIDOR` desde otra PC de la red).

**Comandos frecuentes:**

```bash
docker compose up -d            # levantar
docker compose down             # detener (conserva datos)
docker compose up -d --build    # reconstruir tras cambios de código
docker compose logs -f backend  # ver logs del backend
docker compose ps               # estado de los contenedores
```

---

### 12. Base de datos: inicialización, respaldos y migración

**Inicialización.** En el **primer arranque** (volumen vacío), PostgreSQL ejecuta
`docker/postgres/init/01-init.sql`, que crea el esquema y los datos base (roles, unidades,
categorías y usuario `admin`). Si el volumen ya existe, el script **no** se vuelve a
ejecutar.

**Persistencia.** Los datos viven en el volumen `postgres_data`.

- `docker compose down` conserva los datos.
- `docker compose down -v` **elimina el volumen y borra la base de datos**. Usar con cuidado.

**Respaldos.**

```bash
./scripts/backup-db.sh          # genera docker/backups/tienda_barrio_db_<fecha>.sql
./scripts/restore-db.sh docker/backups/ARCHIVO.sql
```

El respaldo se genera con `pg_dump --clean --if-exists`, por lo que puede restaurarse sobre
una base ya existente sin conflictos.

**Migración a otra PC.** Exportar con `backup-db.sh` en la máquina de origen, copiar el
archivo a la máquina destino y restaurarlo con `restore-db.sh` después de levantar el
sistema. El detalle está en `README-Docker.md`.

> En Windows, los scripts se ejecutan desde **Git Bash** o **WSL**.

---

### 13. Ejecución en desarrollo

La ejecución local (sin Docker) sigue disponible para desarrollo:

**Backend** (requiere Java 21, Maven y una base PostgreSQL local):

```bash
cd backend
mvn spring-boot:run
```

Usa las variables `DB_*` (o sus valores por defecto) definidas en `application.yml`.

**Frontend** (requiere Node y npm):

```bash
cd frontend
npm install
npm start
```

`npm start` (modo desarrollo) usa `environment.ts`, que resuelve la API por el *host* actual.
La build de producción (Docker) usa `environment.prod.ts` con `apiUrl: '/api'`. Ambos
entornos conviven sin interferir.

---

### 14. Operación y mantenimiento

- **Logs:** `docker compose logs -f <servicio>` para diagnóstico.
- **Salud del backend:** `GET /actuator/health` (directo o vía `http://SERVIDOR/actuator/health`).
- **Respaldos periódicos:** programar `backup-db.sh` según la frecuencia deseada.
- **Actualización de la aplicación:** actualizar el código y ejecutar
  `docker compose up -d --build`. El esquema no se recrea (los datos se conservan).
- **Reinicio de credenciales:** el usuario `admin` inicial es `admin` / `Admin123`; se
  recomienda cambiarlo tras la puesta en marcha.
- **Cambios de esquema:** como `ddl-auto=validate`, cualquier cambio en las entidades debe
  reflejarse también en el SQL de la base (migración manual).

---

### 15. Consideraciones a futuro

- **Dominio y HTTPS.** El sistema opera por `http://localhost` o `http://IP_DEL_SERVIDOR`.
  Publicarlo en un dominio con HTTPS requiere: dominio, DNS, certificados SSL, un *reverse
  proxy* adicional, IP pública o un VPS, y endurecimiento de seguridad.
- **Copias de seguridad externas.** Almacenar los respaldos fuera de la PC servidor.
- **Migraciones formales.** Para evolucionar el esquema de forma controlada, se podría
  incorporar una herramienta de migraciones (por ejemplo, Flyway o Liquibase).
- **Monitoreo.** Aprovechar Actuator para métricas y alertas.

---

*Fin del manual técnico.*

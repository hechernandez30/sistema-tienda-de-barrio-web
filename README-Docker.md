# Ejecución con Docker

Guía para levantar **Sistema Tienda de Barrio (Variedades Hernández)** completo en una PC
usando Docker Compose: base de datos PostgreSQL, backend Spring Boot y frontend Angular
servido con Nginx.

---

## Requisitos

- **Docker Desktop** instalado (incluye Docker Compose).
- Puerto **80** libre en la PC servidor.
- **4 GB de RAM** mínimo (8 GB recomendado).
- **10 GB** de espacio libre recomendado.

---

## Instalación rápida

1. Copiar el proyecto a la PC destino.
2. Crear el archivo `.env` a partir del ejemplo:

```bash
cp .env.example .env
```

> En Windows (PowerShell): `Copy-Item .env.example .env`

3. Levantar el sistema (construye las imágenes la primera vez):

```bash
docker compose up -d --build
```

4. Abrir en el navegador:

```text
http://localhost
```

o desde otra PC en la misma red, usando la IP del servidor:

```text
http://IP_DEL_SERVIDOR
```

Ejemplo: `http://192.168.1.50`

> La primera vez puede tardar varios minutos porque compila el backend (Maven) y el
> frontend (Angular). Los siguientes arranques son mucho más rápidos.

---

## Usuario inicial

```text
Usuario: admin
Contraseña: Admin123
```

> La base se inicializa con un usuario `admin` que tiene un hash de contraseña de marcador.
> Al arrancar, el **backend** detecta ese marcador y lo reemplaza automáticamente por el
> hash real (BCrypt) de `Admin123`. No es necesario tocar la base de datos manualmente.

---

## Arquitectura de los servicios

| Servicio   | Imagen base                         | Rol                                      | Puerto host |
| ---------- | ----------------------------------- | ---------------------------------------- | ----------- |
| `postgres` | `postgres:16`                       | Base de datos                            | 5432        |
| `backend`  | `maven` + `eclipse-temurin:21-jre`  | API Spring Boot                          | 8080        |
| `frontend` | `node:20` + `nginx:alpine`          | Angular servido por Nginx + proxy `/api` | **80**      |

- El **frontend es la entrada principal**. El navegador consume la API por ruta relativa
  `/api`, y **Nginx** la redirige internamente a `http://backend:8080/api`.
- El **backend** se conecta a la base de datos por el nombre del servicio Docker
  (`postgres`), **nunca** por `localhost`.
- Publicar el puerto `8080` del backend es opcional (soporte técnico); el uso normal del
  sistema no lo necesita.

---

## Comandos útiles

Levantar (sin reconstruir):

```bash
docker compose up -d
```

Detener (conserva los datos):

```bash
docker compose down
```

Recompilar y levantar (tras cambios en el código):

```bash
docker compose up -d --build
```

Ver logs de todo:

```bash
docker compose logs -f
```

Ver logs por servicio:

```bash
docker compose logs -f backend
docker compose logs -f frontend
docker compose logs -f postgres
```

Estado de los contenedores:

```bash
docker compose ps
```

---

## Respaldos (backups)

Crear un respaldo de la base de datos:

```bash
./scripts/backup-db.sh
```

Genera un archivo en `docker/backups/` con fecha y hora, por ejemplo:
`docker/backups/tienda_barrio_db_20260702_170000.sql`

Restaurar un respaldo:

```bash
./scripts/restore-db.sh docker/backups/NOMBRE_DEL_BACKUP.sql
```

> **Windows:** estos scripts están en Bash. Ejecutarlos desde **Git Bash** o **WSL**.
> Si es necesario, dar permisos de ejecución en Linux/Mac:
> `chmod +x scripts/backup-db.sh scripts/restore-db.sh`

---

## Importante sobre los datos (persistencia)

La base de datos se guarda en un **volumen persistente** (`postgres_data`).

- Este comando **conserva** los datos:

```bash
docker compose down
```

- Este comando **elimina el volumen y borra la base de datos**:

```bash
docker compose down -v
```

> ⚠️ **No ejecutar `docker compose down -v` salvo que se quiera borrar la base de datos.**

### Sobre el script de inicialización

El script `docker/postgres/init/01-init.sql` (crea tablas y datos base) **solo se ejecuta
la primera vez**, cuando el volumen está vacío. Si el volumen ya existe, PostgreSQL **no**
vuelve a ejecutarlo. Para reinicializar desde cero hay que eliminar el volumen
(`docker compose down -v`), lo que **borra todos los datos**.

---

## Acceso desde varias computadoras

Varios usuarios pueden usar el sistema **al mismo tiempo** desde la misma red local. Todos
acceden al mismo frontend, backend y base de datos centralizados en la PC servidor:

```text
PC servidor:      http://localhost
PC cajera:        http://192.168.1.50
PC administrador: http://192.168.1.50
PC inventario:    http://192.168.1.50
```

Recomendaciones:

- Asignar una **IP fija local** (o reserva DHCP en el router) a la PC servidor, para que la
  dirección no cambie.
- Verificar que el **Firewall** de la PC servidor permita el puerto **80** en la red privada.
- Todas las PC deben estar en la **misma red** que el servidor.

---

## Múltiples usuarios

La arquitectura permite múltiples usuarios simultáneos porque el frontend, el backend y la
base de datos están **centralizados** en la PC servidor. Cada PC cliente solo abre el
navegador; no instala nada. La sesión de cada usuario es independiente (autenticación JWT).

---

## Prueba funcional mínima

Después de levantar Docker, validar el flujo básico:

1. Abrir el **login**.
2. Iniciar sesión con `admin` / `Admin123`.
3. Crear un **producto**.
4. **Abrir caja**.
5. Registrar una **venta** en el Punto de Venta.
6. Revisar **inventario**.
7. Revisar **caja**.
8. Revisar **reportes**.

---

## Dominio personalizado (a futuro)

Inicialmente el sistema funciona por:

```text
http://localhost
```

o:

```text
http://IP_DEL_SERVIDOR
```

Usar un dominio como `https://variedades-hernandez.com` **requiere configuración adicional**
que **no** está incluida todavía:

- Dominio comprado.
- Configuración de **DNS**.
- **HTTPS/SSL** (certificados).
- **Reverse proxy** adicional.
- **IP pública** o un **VPS**.
- Endurecimiento de **seguridad**.

Por ahora **no** se implementa dominio ni HTTPS.

---

## Solución de problemas

- **El puerto 80 está ocupado:** cambiar `FRONTEND_PORT` en `.env` (por ejemplo `8081`) y
  entrar por `http://localhost:8081`. También puede ocuparlo IIS, Skype u otro servidor web.
- **El backend no conecta a la base:** revisar `docker compose logs -f backend`. Debe
  esperar a que `postgres` esté *healthy* (ya está configurado con `depends_on`).
- **Cambié el código y no veo cambios:** reconstruir con `docker compose up -d --build`.
- **Quiero empezar de cero (borra datos):** `docker compose down -v` y luego
  `docker compose up -d --build`.
- **No puedo entrar desde otra PC:** revisar Firewall (puerto 80, perfil de red privada) y
  que la IP del servidor sea la correcta.

---

## Desarrollo local (sin Docker)

La ejecución local de desarrollo **sigue funcionando igual**:

- Backend: `mvn spring-boot:run` en `backend/` (usa las variables `DB_*` o sus valores por
  defecto).
- Frontend: `npm start` en `frontend/` (usa `environment.ts`, con la URL de API dinámica).

La build de producción (la que usa Docker) usa `environment.prod.ts` con `apiUrl: '/api'`.
Ambos entornos conviven sin romper nada.

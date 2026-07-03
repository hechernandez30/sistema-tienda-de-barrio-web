#!/bin/bash
# ============================================================
# Respaldo de la base de datos PostgreSQL (dentro de Docker).
# Uso:  ./scripts/backup-db.sh
# En Windows: ejecutar desde Git Bash o WSL.
# ============================================================
set -e

mkdir -p docker/backups

TIMESTAMP=$(date +"%Y%m%d_%H%M%S")
BACKUP_FILE="docker/backups/tienda_barrio_db_$TIMESTAMP.sql"

# --clean --if-exists: el respaldo incluye las órdenes para limpiar los objetos
# existentes antes de recrearlos, de modo que se pueda restaurar sobre una base ya
# inicializada (por ejemplo, en otra PC recién levantada) sin conflictos.
docker compose exec -T postgres pg_dump --clean --if-exists -U tienda_user tienda_barrio_db > "$BACKUP_FILE"

echo "Backup creado: $BACKUP_FILE"

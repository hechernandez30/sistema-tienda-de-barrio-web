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

docker compose exec -T postgres pg_dump -U tienda_user tienda_barrio_db > "$BACKUP_FILE"

echo "Backup creado: $BACKUP_FILE"

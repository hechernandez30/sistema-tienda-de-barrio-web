#!/bin/bash
# ============================================================
# Restauración de la base de datos PostgreSQL (dentro de Docker).
# Uso:  ./scripts/restore-db.sh docker/backups/archivo.sql
# En Windows: ejecutar desde Git Bash o WSL.
# ============================================================
set -e

if [ -z "$1" ]; then
  echo "Uso: ./scripts/restore-db.sh docker/backups/archivo.sql"
  exit 1
fi

BACKUP_FILE="$1"

if [ ! -f "$BACKUP_FILE" ]; then
  echo "No se encontró el archivo: $BACKUP_FILE"
  exit 1
fi

cat "$BACKUP_FILE" | docker compose exec -T postgres psql -U tienda_user -d tienda_barrio_db

echo "Backup restaurado desde: $BACKUP_FILE"

#!/usr/bin/env bash
# Резервное копирование Neo4j (п.6 Администрирование)
# Community: копирование тома данных. Консистентный бэкап — при остановленном Neo4j.

set -e
BACKUP_DIR="${BACKUP_DIR:-./neo4j_backups}"
STAMP=$(date +%Y%m%d_%H%M%S)
mkdir -p "$BACKUP_DIR"
VOLUME_NAME="${NEO4J_VOLUME:-databases_course4_neo4j_data}"

echo "=== Neo4j backup ==="
echo "Backup dir: $BACKUP_DIR"
echo "Stamp: $STAMP"

# Пробуем скопировать из контейнера (если Neo4j остановлен — контейнер может не существовать)
if docker cp library_neo4j:/data "$BACKUP_DIR/neo4j_data_$STAMP" 2>/dev/null; then
  echo "Done: copied from container to $BACKUP_DIR/neo4j_data_$STAMP"
  exit 0
fi

# Копирование тома в tar (работает при запущенном или остановленном Neo4j; консистентность лучше при остановленном)
echo "Copying volume $VOLUME_NAME to $BACKUP_DIR/neo4j_data_$STAMP.tar.gz ..."
docker run --rm \
  -v "$VOLUME_NAME:/data:ro" \
  -v "$(pwd)/$BACKUP_DIR:/backup" \
  alpine tar czf "/backup/neo4j_data_$STAMP.tar.gz" -C /data . 2>/dev/null && {
  echo "Done: $BACKUP_DIR/neo4j_data_$STAMP.tar.gz"
  exit 0
}

echo "For consistent backup, stop Neo4j first: docker compose stop neo4j"
echo "Then run this script again."
exit 1

set -e
BACKUP_DIR="${BACKUP_DIR:-./neo4j_backups}"
STAMP=$(date +%Y%m%d_%H%M%S)
mkdir -p "$BACKUP_DIR"

echo "Creating backup in $BACKUP_DIR/neo4j_$STAMP ..."

docker cp library_neo4j:/data "$BACKUP_DIR/neo4j_data_$STAMP" 2>/dev/null || {
  echo "If Neo4j is running, for consistent backup stop it first: docker compose stop neo4j"
  echo "Then copy volume: docker run --rm -v databases_course4_neo4j_data:/data -v $(pwd)/$BACKUP_DIR:/backup alpine tar czf /backup/neo4j_data_$STAMP.tar.gz -C /data ."
}

echo "Done. Backup at $BACKUP_DIR/neo4j_data_$STAMP (or run the suggested docker run command for volume backup)."

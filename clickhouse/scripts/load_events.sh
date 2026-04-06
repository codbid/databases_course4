#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
DATA_FILE="${1:-$ROOT_DIR/clickhouse/data/book_events.ndjson}"
TOPIC="library.book-events"

if [[ ! -f "$DATA_FILE" ]]; then
  echo "Data file not found: $DATA_FILE" >&2
  exit 1
fi

docker exec kafka1 kafka-topics \
  --create \
  --if-not-exists \
  --topic "$TOPIC" \
  --bootstrap-server kafka1:9092 \
  --partitions 3 \
  --replication-factor 1 >/dev/null

docker exec -i kafka1 bash -lc \
  "kafka-console-producer --bootstrap-server kafka1:9092 --topic $TOPIC >/dev/null" < "$DATA_FILE"

echo "Rows in file: $(wc -l < "$DATA_FILE")"
echo "Rows currently in ClickHouse:"
docker exec library_clickhouse clickhouse-client \
  --user default \
  --password clickhouse \
  --query "SELECT count() FROM library_analytics.book_events WHERE source = 'synthetic-load'"

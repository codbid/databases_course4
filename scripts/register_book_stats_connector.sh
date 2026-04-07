#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
CONNECT_URL="${CONNECT_URL:-http://localhost:8083}"
CONNECTOR_NAME="book-stats-jdbc-sink"
CONFIG_FILE="$ROOT_DIR/src/main/resources/kafka-connect/book-stats-jdbc-sink.json"

if [[ ! -f "$CONFIG_FILE" ]]; then
  echo "Connector config not found: $CONFIG_FILE" >&2
  exit 1
fi

curl -fsS -X PUT \
  -H "Content-Type: application/json" \
  --data @"$CONFIG_FILE" \
  "$CONNECT_URL/connectors/$CONNECTOR_NAME/config"

echo
echo "Connector $CONNECTOR_NAME registered."

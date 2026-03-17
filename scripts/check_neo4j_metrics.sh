#!/usr/bin/env bash
# Скрипт проверки метрик Neo4j: читает из neo4j-exporter или Prometheus и выводит значения.
# Запуск: ./scripts/check_neo4j_metrics.sh
# Переменные: EXPORTER_URL (по умолчанию http://localhost:5000), PROMETHEUS_URL (http://localhost:9090)

set -e
EXPORTER_URL="${EXPORTER_URL:-http://localhost:5000}"
PROMETHEUS_URL="${PROMETHEUS_URL:-http://localhost:9090}"

echo "=============================================="
echo "Neo4j metrics check"
echo "=============================================="

# Парсинг одной метрики из вывода экспортера (строка вида "neo4j_nodes_total 62.0")
get_metric() {
  local name="$1"
  local body="$2"
  echo "$body" | grep -E "^${name}\s+" | awk '{print $2}' | tr -d '\r' || echo ""
}

# 1) Пробуем взять метрики из neo4j-exporter
BODY=""
if command -v curl &>/dev/null; then
  BODY=$(curl -s --connect-timeout 3 "$EXPORTER_URL/metrics" 2>/dev/null) || true
fi

if [ -n "$BODY" ] && echo "$BODY" | grep -q "neo4j_"; then
  echo "Source: neo4j-exporter ($EXPORTER_URL)"
  UP=$(get_metric "neo4j_up" "$BODY")
  NODES=$(get_metric "neo4j_nodes_total" "$BODY")
  RELS=$(get_metric "neo4j_relationships_total" "$BODY")

  [ -z "$UP" ] && UP="(empty)"
  [ -z "$NODES" ] && NODES="(empty)"
  [ -z "$RELS" ] && RELS="(empty)"

  echo "  neo4j_up               = $UP"
  echo "  neo4j_nodes_total      = $NODES"
  echo "  neo4j_relationships_total = $RELS"

  if [ "$UP" = "1" ] || [ "$UP" = "1.0" ]; then
    echo "  Status: Neo4j reachable"
  else
    echo "  Status: Neo4j unreachable or exporter error"
  fi
  echo "=============================================="
  exit 0
fi

# 2) Fallback: запрос к Prometheus API
echo "Exporter not reachable at $EXPORTER_URL, trying Prometheus ($PROMETHEUS_URL) ..."
for metric in neo4j_up neo4j_nodes_total neo4j_relationships_total; do
  RAW=$(curl -s --connect-timeout 3 "${PROMETHEUS_URL}/api/v1/query?query=${metric}" 2>/dev/null) || true
  VAL=$(echo "$RAW" | python3 -c "
import sys, json
try:
    d = json.load(sys.stdin)
    r = d.get('data', {}).get('result', [])
    print(r[0]['value'][1] if r else '')
except Exception:
    print('')
" 2>/dev/null) || true
  [ -z "$VAL" ] && VAL="(no data)"
  echo "  $metric = $VAL"
done
echo "=============================================="

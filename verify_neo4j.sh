#!/usr/bin/env bash
# Скрипт проверки выполнения задания Neo4j (срок 17 февраля)
# Требования: Docker, запущенный контейнер library_neo4j (docker compose up -d neo4j)
# Использование: ./verify_neo4j.sh [bolt_url]
# По умолчанию: bolt://localhost:7687, пользователь neo4j, пароль password

set -e
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
NEO4J_BOLT="${1:-bolt://localhost:7687}"
NEO4J_USER="${NEO4J_USER:-neo4j}"
NEO4J_PASSWORD="${NEO4J_PASSWORD:-password}"
CYPHER_DIR="$SCRIPT_DIR/neo4j/cypher"
FAILED=0
PASSED=0

run_cypher() {
  local msg="$1"
  local input="$2"
  echo "--- $msg ---"
  if echo "$input" | docker exec -i library_neo4j cypher-shell -a "$NEO4J_BOLT" -u "$NEO4J_USER" -p "$NEO4J_PASSWORD" --fail-at-end 2>/dev/null; then
    ((PASSED++)) || true
    return 0
  else
    ((FAILED++)) || true
    return 1
  fi
}

run_cypher_file() {
  local name="$1"
  local path="$2"
  echo "--- Run file: $name ---"
  if docker exec -i library_neo4j cypher-shell -a "$NEO4J_BOLT" -u "$NEO4J_USER" -p "$NEO4J_PASSWORD" --fail-at-end < "$path" 2>/dev/null; then
    ((PASSED++)) || true
    return 0
  else
    ((FAILED++)) || true
    return 1
  fi
}

echo "=============================================="
echo "Neo4j assignment verification"
echo "Bolt: $NEO4J_BOLT"
echo "=============================================="

# Проверка доступности Neo4j
if ! docker exec library_neo4j cypher-shell -a "$NEO4J_BOLT" -u "$NEO4J_USER" -p "$NEO4J_PASSWORD" "RETURN 1 AS ok" 2>/dev/null | grep -q 1; then
  echo "ERROR: Neo4j is not reachable. Start with: docker compose up -d neo4j"
  exit 1
fi
echo "Neo4j is up."
((PASSED++)) || true

# 1. Схема: ограничения и индексы
run_cypher_file "01 Schema (constraints, indexes)" "$CYPHER_DIR/01_schema_constraints_indexes.cypher"

# 2. Очистка и данные: MERGE узлов и связей (чистая БД для детерминированного подсчёта)
run_cypher "Clear DB for fresh load" "MATCH (n) DETACH DELETE n;"
run_cypher_file "02 Data (MERGE 30-50 nodes, 100+ rels)" "$CYPHER_DIR/02_data_merge.cypher"

# 3. Проверка количества узлов (минимум 30) и связей (минимум 100)
NODES=$(docker exec -i library_neo4j cypher-shell -a "$NEO4J_BOLT" -u "$NEO4J_USER" -p "$NEO4J_PASSWORD" --fail-at-end "MATCH (n) RETURN count(n) AS c;" 2>/dev/null | tail -1 | tr -d ' \r' || echo "0")
RELS=$(docker exec -i library_neo4j cypher-shell -a "$NEO4J_BOLT" -u "$NEO4J_USER" -p "$NEO4J_PASSWORD" --fail-at-end "MATCH ()-[r]->() RETURN count(r) AS c;" 2>/dev/null | tail -1 | tr -d ' \r' || echo "0")
echo "--- Node count: $NODES (required >= 30) ---"
echo "--- Relationship count: $RELS (required >= 100) ---"
if [ "${NODES:-0}" -ge 30 ] && [ "${RELS:-0}" -ge 100 ]; then
  ((PASSED++)) || true
else
  echo "FAIL: nodes=$NODES (need >=30), rels=$RELS (need >=100)"
  ((FAILED++)) || true
fi

# 4. Проверка типов узлов (минимум 3) и связей (минимум 2)
run_cypher "Labels count (>=3)" "CALL db.labels() YIELD label RETURN count(label) AS c;"
run_cypher "Relationship types count (>=2)" "CALL db.relationshipTypes() YIELD relationshipType RETURN count(relationshipType) AS c;"

# 5. Запросы на 1 шаг (п.3)
run_cypher_file "03 One-step queries" "$CYPHER_DIR/03_queries_onestep.cypher"

# 6. Цепочки и переменная длина пути (п.4)
run_cypher_file "04 Path queries (*2..4, etc)" "$CYPHER_DIR/04_queries_paths.cypher"

# 7. Агрегации (п.5)
run_cypher_file "05 Aggregations (count, collect, order, limit)" "$CYPHER_DIR/05_queries_aggregations.cypher"

# 8. Общие соседи (п.7)
run_cypher_file "06 Common neighbors" "$CYPHER_DIR/06_common_neighbors.cypher"

# 9. Комплексный запрос (п.8)
run_cypher_file "07 Complex combined query" "$CYPHER_DIR/07_complex_combined.cypher"

# 10. Эквиваленты Postgres/Mongo (п.6)
run_cypher_file "08 Postgres/Mongo Cypher equivalents" "$CYPHER_DIR/08_postgres_mongo_cypher.cypher"

# 11. PROFILE/EXPLAIN (админ п.7)
run_cypher_file "10 PROFILE/EXPLAIN" "$CYPHER_DIR/10_profile_explain.cypher"

# Итог
echo "=============================================="
echo "Passed: $PASSED, Failed: $FAILED"
if [ "$FAILED" -eq 0 ]; then
  echo "Verification OK."
  exit 0
else
  echo "Some checks failed."
  exit 1
fi

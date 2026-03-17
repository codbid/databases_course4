#!/usr/bin/env bash
# Администрирование п.8: моделирование отказа Neo4j (standalone)
# Останавливает контейнер Neo4j; по запросу снова запускает.

set -e
CONTAINER="${NEO4J_CONTAINER:-library_neo4j}"

case "${1:-stop}" in
  stop)
    echo "Stopping Neo4j ($CONTAINER) to simulate failure..."
    docker stop "$CONTAINER" 2>/dev/null || true
    echo "Neo4j stopped. Try connecting via Bolt (e.g. Neo4j Browser or app) — connection will fail."
    echo "To restore: $0 start"
    ;;
  start)
    echo "Starting Neo4j ($CONTAINER)..."
    docker start "$CONTAINER"
    echo "Neo4j started. Wait ~20s then check http://localhost:7474"
    ;;
  *)
    echo "Usage: $0 stop|start"
    exit 1
    ;;
esac

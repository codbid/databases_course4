# Grafana: метрики Neo4j и др.

## Что сделано

- **Провижининг источника данных**: при старте Grafana автоматически добавляется источник **Prometheus** (`http://prometheus:9090`) с uid `prometheus`.
- **Дашборд «Neo4j — Library»**: панели по метрикам Neo4j (доступность, число узлов и связей, графики во времени). Данные берутся из **neo4j-exporter** (sidecar для Neo4j Community, т.к. встроенный Prometheus только в Enterprise).

## Запуск

1. Запустить стек (Neo4j, экспортер, Prometheus, Grafana):  
   `docker-compose up -d neo4j neo4j-exporter prometheus grafana`
2. Подождать ~15 сек, открыть Grafana: http://localhost:3000 (логин `admin`, пароль `admin`).
3. В меню: **Dashboards** → **Neo4j — Library**.

Метрики: `neo4j_up`, `neo4j_nodes_total`, `neo4j_relationships_total` (источник — job `neo4j-metrics`).

## Если панели пустые

- Убедитесь, что запущены `neo4j` и `neo4j-exporter`. Проверка: `curl http://localhost:5000/metrics` — должны быть строки `neo4j_*`.
- В Grafana → **Explore** (Prometheus) выполните запрос `neo4j_up` или `neo4j_nodes_total`.

## Структура

- `provisioning/datasources/` — источник Prometheus.
- `provisioning/dashboards/default.yaml` — провайдер дашбордов (путь ` /var/lib/grafana/dashboards`).
- `dashboards/neo4j.json` — дашборд Neo4j.

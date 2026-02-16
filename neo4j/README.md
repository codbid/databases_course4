# Задание Neo4j (срок 17 февраля)

## Структура

- **cypher/** — скрипты Cypher:
  - `01_schema_constraints_indexes.cypher` — ограничения и индексы
  - `02_data_merge.cypher` — узлы и связи (MERGE, 30–50 узлов, 100+ связей)
  - `03_queries_onestep.cypher` — запросы на 1 шаг (п.3)
  - `04_queries_paths.cypher` — цепочки и переменная длина пути (п.4)
  - `05_queries_aggregations.cypher` — агрегации (п.5)
  - `06_common_neighbors.cypher` — общие соседи (п.7)
  - `07_complex_combined.cypher` — комплексный запрос (п.8)
  - `08_postgres_mongo_cypher.cypher` — эквиваленты Postgres/Mongo (п.6)
  - `09_import_load_csv.cypher` — импорт LOAD CSV (админ п.3)
  - `10_profile_explain.cypher` — PROFILE/EXPLAIN (админ п.7)
- **import/** — CSV для LOAD CSV
- **admin/** — пользователи/роли, резервное копирование
- **docs/** — ответы на вопросы (Neo4j vs Postgres/Mongo, администрирование)

## Запуск и проверка

1. Запустить Neo4j: из корня проекта  
   `docker-compose up -d neo4j` или `docker compose up -d neo4j`
2. Подождать ~30 сек, затем выполнить проверку:  
   `./verify_neo4j.sh`
3. Браузер: http://localhost:7474 (логин `neo4j`, пароль `password`)

## Администрирование

- Пользователи/роли: см. `admin/01_create_users_roles.cypher` (полный RBAC — Neo4j Enterprise).
- Импорт: положить CSV в `neo4j/import/`, выполнить `09_import_load_csv.cypher`.
- Резервное копирование: `./admin/backup_neo4j.sh`
- Мониторинг: Prometheus — порт 2000 (см. `prometheus.yml` в корне проекта).

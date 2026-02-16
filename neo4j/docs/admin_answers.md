# Администрирование Neo4j — ответы и заметки

## 1. Развёртывание в Docker, каталоги, порты, пароль

- **Образ**: `neo4j:5`.
- **Порты**: 7474 (Browser), 7687 (Bolt), 2000 (Prometheus).
- **Каталоги**: `neo4j_data:/data`, `neo4j_logs:/logs`, `./neo4j/import` для LOAD CSV, `./neo4j/plugins` для плагинов.
- **Пароль администратора**: задаётся через `NEO4J_AUTH=neo4j/password` (логин `neo4j`).
- База по умолчанию — `neo4j`. Имя БД проекта можно не менять (в Community одна БД) или задать через переменные окружения при необходимости.

## 2. Пользователи и роли (reader, publisher)

В **Neo4j Community** нет встроенных ролей `reader`/`publisher`; полный RBAC есть в **Enterprise**. В Community можно:

- Создать пользователей: `CREATE USER reader SET PASSWORD 'reader123' CHANGE NOT REQUIRED;`
- В Enterprise: `GRANT ROLE reader TO reader;` (роль `reader` — только чтение), затем подключаться под `reader` и пытаться писать — ожидаемо ошибка; под `publisher` — чтение разрешено.

Для выполнения пункта задания в учебных целях можно использовать образ **neo4j:5-enterprise** (если есть лицензия) или описать в отчёте, что роли продемонстрированы в документации/скриптах, а фактическое ограничение записи работает в Enterprise.

Пример (Enterprise):
```cypher
CREATE USER reader SET PASSWORD 'reader123' CHANGE NOT REQUIRED;
CREATE USER publisher SET PASSWORD 'publisher123' CHANGE NOT REQUIRED;
GRANT ROLE reader TO reader;   -- только чтение
GRANT ROLE publisher TO publisher;  -- по имени роли с правом публикации/записи (если настроена)
-- Под reader: CREATE (n:Test {id:1}) — ожидаемо ошибка доступа.
-- Под publisher: MATCH (n) RETURN count(n) — чтение разрешено.
```

## 3. Импорт через LOAD CSV и database import

- **LOAD CSV**: файлы кладутся в `./neo4j/import` (см. volumes). Запрос вида:
  `LOAD CSV WITH HEADERS FROM 'file:///offices.csv' AS row MERGE (o:Office {id: row.id}) SET o.name = row.name;`
- **Database import (neo4j-admin)**: для начальной загрузки дампов или миграции. Пример: `neo4j-admin database load ...` (см. официальную документацию по версии).

В репозитории добавлен пример CSV в `neo4j/import/` и скрипт `neo4j/cypher/09_import_load_csv.cypher`.

## 4. Ограничения и индексы

- **Ограничения**: в `01_schema_constraints_indexes.cypher` созданы уникальность (`email`, `isbn`, `name`) и существование (`name`, `title`). В Neo4j также есть ограничения на существование свойства, уникальность — полный список в документации.
- **Индексы**: созданы на `Client.city`, `Book.genre`, `Book.year`, `Office.name`. Они ускоряют фильтрацию в MATCH и сортировку. Влияние можно посмотреть через `PROFILE`/`EXPLAIN` запроса до/после создания индекса.

## 5. Мониторинг

- Prometheus-метрики включены на порту 2000; в `prometheus.yml` добавлен job `neo4j-metrics` с target `neo4j:2000` (в сети docker-compose).
- Grafana: дашборды для Neo4j можно импортировать из сообщества (поиск "Neo4j" в grafana.com/dashboards).

## 6. Резервное копирование

- **Онлайн-бэкап (Enterprise)**: `neo4j-admin backup --backup-dir=/backups ...`
- **Community**: остановить Neo4j и скопировать каталог `data/` (или использовать `neo4j-admin database dump` в поддерживаемых версиях). В скрипте `neo4j/admin/backup_neo4j.sh` приведён пример копирования тома/каталога.

## 7. Производительность: память, PROFILE, EXPLAIN

- В docker-compose заданы: `NEO4J_dbms_memory_heap_initial__size=512m`, `NEO4J_dbms_memory_heap_max__size=512m`, `NEO4J_dbms_memory_pagecache_size=256m`. Для продакшена значения увеличивают по рекомендациям Neo4j.
- **PROFILE** — выполнить запрос и вывести реальные метрики (строки, db hits).
- **EXPLAIN** — только план без выполнения. В скрипте проверки вызываются примеры PROFILE/EXPLAIN для одного запроса.

## 8. Отказоустойчивость: standalone / cluster

- **Standalone (single-node)**: один инстанс. Подходит для разработки и небольших нагрузок. Риск — отказ узла = полная недоступность.
- **Cluster (Causal / Core-edge в Enterprise)**: несколько узлов, репликация, отказоустойчивость. Нужен при требовании к доступности и масштабировании записи/чтения.
- **Смоделировать отказ**: остановить контейнер Neo4j (`docker stop library_neo4j`) и убедиться, что приложение не может подключиться к Bolt; после `docker start library_neo4j` — восстановление.
- **Риски при росте графа**: увеличение объёма данных и глубины обхода ведёт к росту времени запросов с `*` (переменная длина пути); важно ограничивать глубину, использовать индексы и фильтры, мониторить память и page cache.

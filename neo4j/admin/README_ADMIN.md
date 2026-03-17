# Администрирование Neo4j — выполнение по пунктам

## 1. Развёртывание в Docker, структура каталогов, порты, БД, пароль

**Развёрнуто в** `docker-compose.yaml`:

| Параметр | Значение |
|----------|----------|
| Образ | `neo4j:5` |
| Порты | 7474 (Browser), 7687 (Bolt), 2000 (Prometheus, Enterprise) |
| Данные | том `neo4j_data` → `/data` |
| Логи | том `neo4j_logs` → `/logs` |
| Импорт CSV | `./neo4j/import` → `/var/lib/neo4j/import` |
| Плагины | `./neo4j/plugins` → `/plugins` |
| База по умолчанию | `neo4j` (единственная в Community) |
| Администратор | логин `neo4j`, пароль задаётся в `NEO4J_AUTH=neo4j/password` |

**Запуск:**
```bash
docker-compose up -d neo4j
```

---

## 2. Пользователи reader, publisher. Запись под reader, чтение под publisher

**Важно:** роли `reader` (только чтение) и `publisher` (чтение/запись) есть только в **Neo4j Enterprise**. В Community можно создать пользователей, но разграничение по ролям не применяется.

**Создание пользователей** (под admin `neo4j`):
```bash
docker exec -i library_neo4j cypher-shell -u neo4j -p password < neo4j/admin/01_create_users_roles.cypher
```

В Neo4j Browser под `neo4j` выполнить:
```cypher
CREATE USER reader SET PASSWORD 'reader123' CHANGE NOT REQUIRED;
CREATE USER publisher SET PASSWORD 'publisher123' CHANGE NOT REQUIRED;
```

**В Enterprise** дополнительно:
```cypher
GRANT ROLE reader TO reader;
GRANT ROLE publisher TO publisher;
```

**Проверка:**
- Подключиться как `reader` (Browser: bolt://localhost:7687, user reader, password reader123). Выполнить `CREATE (n:Test {id:1})` — в Enterprise ожидаемо ошибка доступа (роль reader только на чтение).
- Подключиться как `publisher`. Выполнить `MATCH (n) RETURN count(n)` — чтение разрешено.

В Community оба пользователя имеют те же права, что и admin; ограничения показываются только в Enterprise.

---

## 3. Импорт: LOAD CSV и database import

### LOAD CSV
Файлы класть в `neo4j/import/`. Пример уже есть: `offices_import.csv`, запрос в `neo4j/cypher/09_import_load_csv.cypher`.

Выполнить в cypher-shell или Browser:
```bash
docker exec -i library_neo4j cypher-shell -u neo4j -p password < neo4j/cypher/09_import_load_csv.cypher
```
Или в Browser:
```cypher
LOAD CSV WITH HEADERS FROM 'file:///offices_import.csv' AS row
MERGE (o:Office {id: row.id})
SET o.name = row.name, o.address = row.address, o.working_time = row.working_time
RETURN count(o) AS imported;
```

### Database import (neo4j-admin)
Для дампа/восстановления БД (офлайн):

**Дамп** (остановить Neo4j, затем):
```bash
docker run --rm -v neo4j_data:/data -v $(pwd)/neo4j_backups:/backup neo4j:5 neo4j-admin database dump neo4j --to-path=/backup
```

**Загрузка** из дампа:
```bash
neo4j-admin database load neo4j --from-path=/backup
```
Актуальный синтаксис смотри в документации своей версии Neo4j.

---

## 4. Ограничения (уникальность, существование), индексы, влияние на запросы

**Создание** — в `neo4j/cypher/01_schema_constraints_indexes.cypher`:
- Уникальность: `Client.email`, `Book.isbn`, `Author.name`
- Существование свойства (NOT NULL): только в Enterprise (закомментировано в 01)
- Индексы: `Client.city`, `Book.genre`, `Book.year`, `Office.name`

**Какие ещё ограничения бывают в Neo4j:**  
Уникальность, существование свойства, ключ (property key). Список: `SHOW CONSTRAINTS;` в Cypher.

**Просмотр ограничений и индексов:**
```cypher
SHOW CONSTRAINTS;
SHOW INDEXES;
```

**Влияние на запросы** — выполнить `neo4j/admin/04_constraints_indexes_impact.cypher`: там PROFILE запроса с фильтром по полю с индексом (например, `Book.genre`). В плане должно быть использование индекса (NodeIndexSeek и т.п.), меньше db hits по сравнению с полным сканом.

---

## 5. Мониторинг

- **Prometheus:** скрейпит neo4j-exporter (job `neo4j-metrics`, target `neo4j-exporter:5000`). Конфиг в `prometheus.yml`.
- **Grafana:** дашборд «Neo4j — Library», источник — Prometheus.  
  Запуск: `docker-compose up -d neo4j-exporter prometheus grafana`. Открыть http://localhost:3000.

Проверка метрик:
```bash
./scripts/check_neo4j_metrics.sh
```

---

## 6. Резервное копирование

Скрипт: `neo4j/admin/backup_neo4j.sh`.

**Консистентный бэкап (рекомендуется):**
```bash
docker-compose stop neo4j
./neo4j/admin/backup_neo4j.sh
docker-compose start neo4j
```
Бэкап сохраняется в `neo4j_backups/` (или в каталог из переменной `BACKUP_DIR`).

---

## 7. Производительность: память, PROFILE, EXPLAIN

**Память** (в docker-compose):
- `NEO4J_dbms_memory_heap_initial__size=512m`
- `NEO4J_dbms_memory_heap_max__size=512m`
- `NEO4J_dbms_memory_pagecache_size=256m`

**PROFILE и EXPLAIN** — примеры в `neo4j/cypher/10_profile_explain.cypher`. В Neo4j Browser:
```cypher
EXPLAIN MATCH (b:Book) WHERE b.genre = 'классика' RETURN b;
PROFILE MATCH (b:Book) WHERE b.genre = 'классика' RETURN b;
```
EXPLAIN — только план, PROFILE — план + фактические db hits и строки.

---

## 8. Отказоустойчивость: standalone/cluster, моделирование отказа

**Смоделировать отказ:**
```bash
./neo4j/admin/simulate_failure.sh
```
Или вручную: `docker stop library_neo4j` — приложение и cypher-shell не смогут подключиться к Bolt. Затем `docker start library_neo4j` — работа восстанавливается.

**Ответы на вопросы:**

- **Где держать в single-node?** Разработка, тесты, небольшая нагрузка, когда допустим простой при отказе узла.
- **Когда нужен кластер?** Требуется высокая доступность (HA), масштабирование чтения/записи, отказоустойчивость (несколько узлов). Только Neo4j Enterprise.
- **Риски при росте графа:** рост времени запросов с переменной длиной пути (`*`), рост потребления памяти и page cache; нужны индексы, ограничение глубины обхода, мониторинг.

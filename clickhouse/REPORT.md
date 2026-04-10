# Подсистема аналитики на ClickHouse (интеграция с Kafka из приложения)

## 1) Типы событий, поля и бизнес-вопросы

Формат событий соответствует `EventBuilder` и топику `library.book-events`
(`src/main/kotlin/app/kafka/EventBuilder.kt`, `src/main/kotlin/app/kafka/KafkaTopics.kt`).

### Событие: `BookLoaned`
Поля:
- `event_id`, `event_time`, `event_type`, `entity_id`, `source`, `version`
- `loan_id`, `book_copy_id`, `client_id`

Бизнес-вопросы:
- Какие клиенты берут больше всего книг?
- Какие экземпляры чаще всего выдаются?
- В какие дни/часы пики выдач?

### Событие: `ReservationCreated`
Поля:
- `event_id`, `event_time`, `event_type`, `entity_id`, `source`, `version`
- `reservation_id`, `book_copy_id`, `client_id`

Бизнес-вопросы:
- Как меняется спрос на бронирования по неделям?
- Какие клиенты чаще бронируют книги?
- Как соотносятся бронирования и выдачи по клиентам?

### Событие: `BookIssued`
Поля:
- `event_id`, `event_time`, `event_type`, `entity_id`, `source`, `version`
- `book_id`, `book_copy_id`, `office_id`, `status`

Бизнес-вопросы:
- В каких офисах чаще пополняется фонд?
- Есть ли всплески выдачи новых экземпляров?
- Какие экземпляры были добавлены в конкретный период?

## 2) Таблица хранения в ClickHouse

DDL: `clickhouse/init/01_events_raw.sql`

Ключевые поля:
- время события: `event_time` + вычисляемые `event_date`, `event_week`
- тип события: `event_type`
- идентификаторы сущностей: `event_id`, `entity_id`, `loan_id`, `reservation_id`, `book_id`, `book_copy_id`, `client_id`, `office_id`
- служебные поля: `ingested_at`, `source`, `version`, `payload_raw`

### Пояснения к `PARTITION BY` и `ORDER BY`
- `PARTITION BY toYYYYMM(event_date)`:
  - аналитика в основном по времени;
  - месячные партиции дают предсказуемые сканы;
  - TTL удаляет устаревшие партиции без full-scan.
- `ORDER BY (event_date, event_type, book_copy_id, client_id, event_id)`:
  - основные фильтры идут по дате и типу события;
  - частые разрезы по экземпляру и клиенту;
  - `event_id` в ключе упрощает дедубликацию.

## 3) Тестовые данные и загрузка

Генератор: `clickhouse/scripts/generate_events.py`  
Загрузчик: `clickhouse/scripts/load_events.ps1`

Свойства данных:
- >100_000 строк на запуск (по умолчанию `--count 120000`);
- повторяющиеся `book_copy_id`, `client_id`, `office_id`;
- дубликаты `event_id` (параметр `--duplicate-rate`);
- всплески активности по часам;
- распределение по нескольким неделям.

Команды:
```bash
python3 clickhouse/scripts/generate_events.py --count 120000
powershell -ExecutionPolicy Bypass -File clickhouse/scripts/load_events.ps1
```

Подтверждение загрузки (скрипт делает запрос и выводит число строк):
```sql
SELECT count()
FROM library_analytics.book_events
WHERE source = 'library-backend';
```

## 4) Базовые аналитические запросы (8+)

SQL: `clickhouse/queries/analytics.sql`

Бизнес-задачи:
1) Дневной объем событий по типам.  
2) Топ клиентов по выдачам.  
3) Уникальные клиенты по неделям.  
4) Пиковые часы активности.  
5) Самые активные экземпляры книг.  
6) Выдача новых экземпляров по офисам.  
7) Соотношение бронирований и выдач по клиентам.  
8) Контроль дубликатов по `event_id`.  
9) Тренд через агрегированную витрину.  
10) Сравнение сырого запроса и витрины.

## 5) Агрегированная витрина

DDL и MV: `clickhouse/init/02_daily_event_mart.sql`

Пример запроса с витриной:
```sql
SELECT day, event_type, office_id, sum(events) AS total_events
FROM library_analytics.daily_event_metrics
WHERE day >= today() - 14
GROUP BY day, event_type, office_id
ORDER BY day, event_type, office_id;
```

## 6) Дедубликация / обновление данных

Сценарий: возможна повторная доставка событий в Kafka.  
Решение: `ReplacingMergeTree(ingested_at)` и дедубликация по ключу.

```sql
SELECT count(), uniqExact(event_id)
FROM library_analytics.book_events
WHERE source = 'library-backend';

OPTIMIZE TABLE library_analytics.book_events FINAL DEDUPLICATE;
```

## 7) TTL и политика хранения

В `clickhouse/init/01_events_raw.sql`:
- сырой слой: 6 месяцев.

В `clickhouse/init/02_daily_event_mart.sql`:
- витрина: 12 месяцев.

Причина:
- сырые события полезны для расследований и точной аналитики;
- агрегаты дешевле и нужны для долгих трендов.

## 8) Мини-отчет: метрики, выводы, сравнение

Ключевые метрики:
- дневной объем событий по типам;
- топ клиентов по выдачам;
- уникальные клиенты по неделям;
- пиковые часы активности;
- объем BookIssued по офисам.

Пример аналитических выводов (на синтетике после загрузки):
- пики активности чаще всего в обед и вечером;
- несколько клиентов формируют значительную долю выдач;
- офисы с большим числом BookIssued не всегда совпадают с пиками выдач.

Сравнение сырого запроса и витрины:
```sql
SELECT event_date, countIf(event_type = 'BookIssued') AS raw_issued
FROM library_analytics.book_events
WHERE event_date >= today() - 30
GROUP BY event_date
ORDER BY event_date;
```

```sql
SELECT day, sumIf(events, event_type = 'BookIssued') AS mart_issued
FROM library_analytics.daily_event_metrics
WHERE day >= today() - 30
GROUP BY day
ORDER BY day;
```

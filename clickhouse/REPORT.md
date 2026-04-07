# ClickHouse Analytics Subsystem

## 1. Event types for analytics

The subsystem consumes events from Kafka topic `library.book-events` produced by the main library backend.

### Event 1: `BookLoaned`
- Fields:
  - `event_id`
  - `event_time`
  - `office_id`
  - `client_id`
  - `book_id`
  - `book_copy_id`
  - `loan_id`
  - `status`
- Business questions:
  - Which offices issue the most loans?
  - What hours or days have peak loan activity?
  - Which books or copies are the most востребованные?

### Event 2: `ReservationCreated`
- Fields:
  - `event_id`
  - `event_time`
  - `office_id`
  - `client_id`
  - `book_id`
  - `book_copy_id`
  - `reservation_id`
- Business questions:
  - How many reservations convert into later loans?
  - Which offices accumulate the most demand?
  - How many unique users reserve books weekly?

### Event 3: `BookIssued`
- Fields:
  - `event_id`
  - `event_time`
  - `office_id`
  - `book_id`
  - `book_copy_id`
  - `status`
- Business questions:
  - How many physical copies enter circulation by office?
  - Are there bursts in inventory updates?
  - Which offices expand inventory fastest?

## 2. Raw ClickHouse table design

DDL is in [01_kafka_ingest.sql](/home/nm/study/databases_course4/clickhouse/init/01_kafka_ingest.sql).

### Why `PARTITION BY toYYYYMM(event_date)`
- Main analytics are time-based.
- Monthly partitioning keeps scans predictable.
- Old partitions are easy to delete with TTL.

### Why `ORDER BY (event_date, event_type, office_id, client_id, book_id, event_id)`
- Typical filters start from date range and event type.
- Many reports drill down by office and client.
- `event_id` in sorting key supports deduplication scenario for repeated event delivery.

### Chosen storage engine
- `ReplacingMergeTree(ingested_at)` on raw data.
- This supports eventual deduplication of repeated events with the same sorting key and `event_id`.

## 3. Aggregated mart

DDL is in [02_daily_office_mart.sql](/home/nm/study/databases_course4/clickhouse/init/02_daily_office_mart.sql).

### Purpose
- Pre-aggregate per day and office:
  - `loan_events`
  - `reservation_events`
  - `issued_events`

### Example mart query
```sql
SELECT day, office_id, loan_events
FROM library_analytics.daily_office_metrics
WHERE day >= today() - 14
ORDER BY day, office_id;
```

## 4. Deduplication scenario

### Scenario
- Kafka can redeliver the same event more than once.
- Synthetic generator intentionally creates duplicate `event_id`.

### SQL approach
```sql
SELECT count(), uniqExact(event_id)
FROM library_analytics.book_events
WHERE source = 'synthetic-load';

OPTIMIZE TABLE library_analytics.book_events FINAL DEDUPLICATE;
```

### Why this is acceptable
- For raw append-heavy analytics data, eventual deduplication is enough.
- It keeps ingestion simple and fast.

## 5. TTL / retention policy

### Rules
- Raw table: keep 6 months.
- Daily mart: keep 12 months.

### Why
- Detailed event-level data is needed for short and medium-term investigations.
- Aggregated mart is cheaper and useful for longer trends.

## 6. Test data

Generator: [generate_events.py](/home/nm/study/databases_course4/clickhouse/scripts/generate_events.py)  
Loader: [load_events.sh](/home/nm/study/databases_course4/clickhouse/scripts/load_events.sh)

Properties:
- more than 100,000 rows per run;
- repeated `client_id`, `book_id`, `office_id`;
- duplicates by `event_id`;
- bursts around specific hours;
- data distributed across several weeks.

## 7. Analytics queries

Queries are collected in [analytics.sql](/home/nm/study/databases_course4/clickhouse/queries/analytics.sql).

Covered operations:
- filtering by time;
- grouping;
- sorting;
- aggregate functions;
- unique values.

## 8. Mini-report

### 3-5 key metrics
- Daily event volume.
- Top offices by loans.
- Weekly unique clients.
- Reservation-to-loan ratio.
- Peak hours of usage.

### Example findings on generated data
- Offices with the highest reservation flow are not always leaders by loans.
- Peak activity concentrates near lunchtime and evening.
- Duplicate events are visible in raw ingestion and can be controlled by dedup queries.

### Raw vs mart comparison
- Raw:
```sql
SELECT event_date, countIf(event_type = 'BookLoaned')
FROM library_analytics.book_events
WHERE office_id = 3
GROUP BY event_date
ORDER BY event_date;
```

- Mart:
```sql
SELECT day, sum(loan_events)
FROM library_analytics.daily_office_metrics
WHERE office_id = 3
GROUP BY day
ORDER BY day;
```

The mart query is simpler and scans fewer rows because aggregation is computed during ingestion.

## 9. Console demo / visual

For a live console demo there is a small terminal dashboard:

- Script: [dashboard.py](/home/nm/study/databases_course4/clickhouse/scripts/dashboard.py)
- Run:
```bash
python3 clickhouse/scripts/dashboard.py
```

The dashboard shows:
- overview metrics;
- event mix by type;
- top offices from the mart;
- peak hours;
- raw vs mart comparison;
- duplicate control query;
- custom SQL execution.

Fast non-interactive mode:
```bash
python3 clickhouse/scripts/dashboard.py --summary
```

## 9.1 Integrated UI and streaming path

The project also contains an integrated web UI in the Ktor backend:

- page: `/pipeline`
- purpose: manual end-to-end verification of the event pipeline

This UI shows:
- raw Kafka topic `library.book-events`
- Kafka Streams output topic `library.book-stats-hourly`
- Kafka Connect sink status
- PostgreSQL sink table populated from Kafka Connect
- ClickHouse raw table and aggregated mart checks

End-to-end path:

`Library backend -> Kafka topic -> Kafka Streams -> Kafka Connect JDBC sink -> PostgreSQL`

Analytics path in parallel:

`Library backend -> Kafka topic -> ClickHouse Kafka engine -> raw table -> materialized view -> daily mart`

## 10. Demo scenario for defense

1. Show the data flow:
`Ktor -> Kafka -> ClickHouse raw table -> materialized view -> daily mart`

2. Show raw DDL:
- [01_kafka_ingest.sql](/home/nm/study/databases_course4/clickhouse/init/01_kafka_ingest.sql)

3. Show aggregated mart:
- [02_daily_office_mart.sql](/home/nm/study/databases_course4/clickhouse/init/02_daily_office_mart.sql)

4. Show generator and bulk load:
- [generate_events.py](/home/nm/study/databases_course4/clickhouse/scripts/generate_events.py)
- [load_events.sh](/home/nm/study/databases_course4/clickhouse/scripts/load_events.sh)

5. Open the dashboard and demonstrate:
- total events and weeks covered;
- distribution of event types;
- top offices by loans;
- raw vs mart for office `3`.

## 11. Short oral explanation

### Architecture
- Backend produces business events into Kafka.
- ClickHouse reads the Kafka topic directly through `Kafka` engine.
- Raw events are stored in `ReplacingMergeTree`.
- Aggregated daily metrics are built by materialized view into a `SummingMergeTree` mart.

### Why this is “good”
- It is part of the same project, not a separate toy example.
- There is a real event flow from backend to analytics storage.
- There is both raw storage and a ready mart for reports.
- There is retention policy, synthetic load, and console demo.

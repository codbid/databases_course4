CREATE DATABASE IF NOT EXISTS library_analytics;

CREATE TABLE IF NOT EXISTS library_analytics.book_events
(
    event_id String,
    event_type LowCardinality(String),
    entity_id String,
    event_time Nullable(DateTime),
    ingested_at DateTime DEFAULT now(),
    event_date Date MATERIALIZED toDate(coalesce(event_time, ingested_at)),
    event_week Date MATERIALIZED toStartOfWeek(coalesce(event_time, ingested_at)),
    source LowCardinality(String),
    version UInt32,
    payload_raw String,
    loan_id Nullable(UInt64),
    reservation_id Nullable(UInt64),
    book_id Nullable(UInt64),
    book_copy_id Nullable(UInt64),
    client_id Nullable(UInt64),
    office_id Nullable(UInt64),
    status Nullable(String)
)
ENGINE = ReplacingMergeTree(ingested_at)
PARTITION BY toYYYYMM(event_date)
ORDER BY (event_date, event_type, ifNull(office_id, 0), ifNull(client_id, 0), ifNull(book_id, 0), event_id)
TTL event_date + INTERVAL 6 MONTH DELETE
SETTINGS index_granularity = 8192;

CREATE TABLE IF NOT EXISTS library_analytics.book_events_kafka
(
    raw String
)
ENGINE = Kafka
SETTINGS
    kafka_broker_list = 'kafka1:9092,kafka2:9092,kafka3:9092',
    kafka_topic_list = 'library.book-events',
    kafka_group_name = 'clickhouse-book-events',
    kafka_format = 'JSONAsString',
    kafka_num_consumers = 1,
    kafka_handle_error_mode = 'stream';

CREATE MATERIALIZED VIEW IF NOT EXISTS library_analytics.book_events_mv
TO library_analytics.book_events
AS
SELECT
    JSONExtractString(raw, 'eventId') AS event_id,
    JSONExtractString(raw, 'eventType') AS event_type,
    JSONExtractString(raw, 'entityId') AS entity_id,
    parseDateTimeBestEffortOrNull(JSONExtractString(raw, 'timestamp')) AS event_time,
    JSONExtractString(raw, 'source') AS source,
    toUInt32OrZero(JSONExtractRaw(raw, 'version')) AS version,
    JSONExtractRaw(raw, 'payload') AS payload_raw,
    JSONExtract(raw, 'payload', 'loanId', 'Nullable(UInt64)') AS loan_id,
    JSONExtract(raw, 'payload', 'reservationId', 'Nullable(UInt64)') AS reservation_id,
    JSONExtract(raw, 'payload', 'bookId', 'Nullable(UInt64)') AS book_id,
    JSONExtract(raw, 'payload', 'bookCopyId', 'Nullable(UInt64)') AS book_copy_id,
    JSONExtract(raw, 'payload', 'clientId', 'Nullable(UInt64)') AS client_id,
    JSONExtract(raw, 'payload', 'officeId', 'Nullable(UInt64)') AS office_id,
    JSONExtract(raw, 'payload', 'status', 'Nullable(String)') AS status
FROM library_analytics.book_events_kafka;

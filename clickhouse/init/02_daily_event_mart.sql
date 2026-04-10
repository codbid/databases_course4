CREATE TABLE IF NOT EXISTS library_analytics.daily_event_metrics
(
    day Date,
    event_type LowCardinality(String),
    office_id UInt64,
    events UInt64
)
ENGINE = SummingMergeTree
PARTITION BY toYYYYMM(day)
ORDER BY (day, event_type, office_id)
TTL day + INTERVAL 12 MONTH DELETE;

CREATE MATERIALIZED VIEW IF NOT EXISTS library_analytics.daily_event_metrics_mv
TO library_analytics.daily_event_metrics
AS
SELECT
    event_date AS day,
    event_type,
    toUInt64(ifNull(office_id, 0)) AS office_id,
    count() AS events
FROM library_analytics.book_events
GROUP BY day, event_type, office_id;

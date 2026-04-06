CREATE TABLE IF NOT EXISTS library_analytics.daily_office_metrics
(
    day Date,
    office_id UInt64,
    loan_events UInt64,
    reservation_events UInt64,
    issued_events UInt64
)
ENGINE = SummingMergeTree
PARTITION BY toYYYYMM(day)
ORDER BY (day, office_id)
TTL day + INTERVAL 12 MONTH DELETE;

CREATE MATERIALIZED VIEW IF NOT EXISTS library_analytics.daily_office_metrics_mv
TO library_analytics.daily_office_metrics
AS
SELECT
    event_date AS day,
    toUInt64(ifNull(office_id, 0)) AS office_id,
    countIf(event_type = 'BookLoaned') AS loan_events,
    countIf(event_type = 'ReservationCreated') AS reservation_events,
    countIf(event_type = 'BookIssued') AS issued_events
FROM library_analytics.book_events
GROUP BY day, office_id;

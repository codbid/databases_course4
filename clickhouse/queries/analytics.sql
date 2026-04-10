-- 1. Daily volume by event type for the last 21 days.
SELECT
    event_date,
    event_type,
    count() AS events
FROM library_analytics.book_events
WHERE event_date >= today() - 21
GROUP BY event_date, event_type
ORDER BY event_date, event_type;

-- 2. Top clients by loan count for the last 30 days.
SELECT
    client_id,
    count() AS loan_events
FROM library_analytics.book_events
WHERE event_type = 'BookLoaned'
  AND event_date >= today() - 30
  AND client_id IS NOT NULL
GROUP BY client_id
ORDER BY loan_events DESC
LIMIT 10;

-- 3. Unique active clients per week.
SELECT
    event_week,
    uniqExact(client_id) AS unique_clients
FROM library_analytics.book_events
WHERE client_id IS NOT NULL
GROUP BY event_week
ORDER BY event_week;

-- 4. Peak hours of activity.
SELECT
    toHour(coalesce(event_time, ingested_at)) AS hour_of_day,
    count() AS events
FROM library_analytics.book_events
GROUP BY hour_of_day
ORDER BY events DESC, hour_of_day;

-- 5. Most active book copies by number of events.
SELECT
    book_copy_id,
    count() AS total_events
FROM library_analytics.book_events
WHERE book_copy_id IS NOT NULL
GROUP BY book_copy_id
ORDER BY total_events DESC
LIMIT 15;

-- 6. Issued events by office and day (only BookIssued carries office_id).
SELECT
    event_date,
    office_id,
    count() AS issued_events
FROM library_analytics.book_events
WHERE event_type = 'BookIssued'
  AND office_id IS NOT NULL
  AND event_date >= today() - 30
GROUP BY event_date, office_id
ORDER BY event_date, issued_events DESC;

-- 7. Reservation to loan ratio per client.
SELECT
    client_id,
    countIf(event_type = 'ReservationCreated') AS reservations,
    countIf(event_type = 'BookLoaned') AS loans,
    round(loans / nullIf(reservations, 0), 3) AS loan_to_reservation_ratio
FROM library_analytics.book_events
WHERE client_id IS NOT NULL
  AND event_date >= today() - 30
GROUP BY client_id
ORDER BY loan_to_reservation_ratio DESC, reservations DESC
LIMIT 20;

-- 8. Duplicate load control by event_id.
SELECT
    count() AS raw_rows,
    uniqExact(event_id) AS unique_event_ids,
    raw_rows - unique_event_ids AS duplicate_rows
FROM library_analytics.book_events
WHERE source = 'library-backend';

-- 9. Office trend through the aggregated mart.
SELECT
    day,
    office_id,
    sumIf(events, event_type = 'BookIssued') AS issued_events
FROM library_analytics.daily_event_metrics
WHERE day >= today() - 30
GROUP BY day, office_id
ORDER BY day, office_id;

-- 10. Raw vs mart comparison (BookIssued per day).
SELECT
    event_date,
    countIf(event_type = 'BookIssued') AS raw_issued
FROM library_analytics.book_events
WHERE event_date >= today() - 30
GROUP BY event_date
ORDER BY event_date;

SELECT
    day,
    sumIf(events, event_type = 'BookIssued') AS mart_issued
FROM library_analytics.daily_event_metrics
WHERE day >= today() - 30
GROUP BY day
ORDER BY day;

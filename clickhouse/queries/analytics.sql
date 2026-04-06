-- 1. Daily volume of all events.
SELECT event_date, count() AS events
FROM library_analytics.book_events
WHERE event_date >= today() - 14
GROUP BY event_date
ORDER BY event_date;

-- 2. Top offices by book loans for the last 7 days.
SELECT office_id, count() AS loans
FROM library_analytics.book_events
WHERE event_type = 'BookLoaned'
  AND event_date >= today() - 7
GROUP BY office_id
ORDER BY loans DESC
LIMIT 10;

-- 3. Unique active clients per week.
SELECT event_week, uniqExact(client_id) AS unique_clients
FROM library_analytics.book_events
WHERE client_id IS NOT NULL
GROUP BY event_week
ORDER BY event_week;

-- 4. Conversion from reservations to loans by office.
SELECT
    office_id,
    countIf(event_type = 'ReservationCreated') AS reservations,
    countIf(event_type = 'BookLoaned') AS loans,
    round(loans / nullIf(reservations, 0), 3) AS loan_to_reservation_ratio
FROM library_analytics.book_events
WHERE event_date >= today() - 30
GROUP BY office_id
ORDER BY loan_to_reservation_ratio DESC, reservations DESC;

-- 5. Top repeated books by number of touch events.
SELECT book_id, count() AS total_events
FROM library_analytics.book_events
WHERE book_id IS NOT NULL
GROUP BY book_id
ORDER BY total_events DESC
LIMIT 15;

-- 6. Peak hours for traffic.
SELECT toHour(coalesce(event_time, ingested_at)) AS hour_of_day, count() AS events
FROM library_analytics.book_events
GROUP BY hour_of_day
ORDER BY events DESC, hour_of_day;

-- 7. Duplicate load control by event_id.
SELECT
    count() AS raw_rows,
    uniqExact(event_id) AS unique_event_ids,
    raw_rows - unique_event_ids AS duplicate_rows
FROM library_analytics.book_events
WHERE source = 'synthetic-load';

-- 8. Office trend through the aggregated mart.
SELECT
    day,
    office_id,
    loan_events,
    reservation_events,
    issued_events
FROM library_analytics.daily_office_metrics
WHERE day >= today() - 14
ORDER BY day, office_id
LIMIT 100;

-- 9. Raw vs mart comparison for office 3.
SELECT
    event_date AS day,
    countIf(event_type = 'BookLoaned') AS raw_loans
FROM library_analytics.book_events
WHERE office_id = 3
  AND event_date >= today() - 14
GROUP BY day
ORDER BY day;

SELECT
    day,
    sum(loan_events) AS mart_loans
FROM library_analytics.daily_office_metrics
WHERE office_id = 3
  AND day >= today() - 14
GROUP BY day
ORDER BY day;

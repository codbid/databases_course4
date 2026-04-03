DROP TABLE IF EXISTS loans_plain CASCADE;
DROP TABLE IF EXISTS loans_part  CASCADE;

CREATE TABLE loans_plain (LIKE public.loans INCLUDING ALL);
INSERT INTO loans_plain SELECT * FROM public.loans;
ANALYZE loans_plain;

CREATE INDEX IF NOT EXISTS loans_plain_starts_at_idx  ON loans_plain (starts_at);
CREATE INDEX IF NOT EXISTS loans_plain_client_id_idx  ON loans_plain (client_id);
CREATE INDEX IF NOT EXISTS loans_plain_book_copy_idx  ON loans_plain (book_copy_id);
CREATE INDEX IF NOT EXISTS loans_plain_status_idx     ON loans_plain (status);

CREATE TABLE loans_part (
                            id            integer NOT NULL,
                            book_copy_id  int     NOT NULL,
                            client_id     int     NOT NULL,
                            status        varchar(255) NOT NULL,
                            starts_at     timestamp NOT NULL,
                            ends_at       timestamp NOT NULL,
                            CONSTRAINT loans_part_fk_clients
                                FOREIGN KEY (client_id) REFERENCES clients(id) ON DELETE RESTRICT,
                            CONSTRAINT loans_part_fk_book_copy
                                FOREIGN KEY (book_copy_id) REFERENCES book_copies(id) ON DELETE CASCADE
) PARTITION BY RANGE (starts_at);

CREATE INDEX IF NOT EXISTS loans_part_starts_at_idx ON loans_part (starts_at);
CREATE INDEX IF NOT EXISTS loans_part_client_id_idx ON loans_part (client_id);
CREATE INDEX IF NOT EXISTS loans_part_book_copy_idx ON loans_part (book_copy_id);
CREATE INDEX IF NOT EXISTS loans_part_status_idx    ON loans_part (status);

DO $$
    DECLARE
        d_from date;
        d_to date;
        next_month date;
        part_name text;
    BEGIN
        SELECT date_trunc('month', min(starts_at))::date,
               date_trunc('month', max(starts_at))::date
        INTO d_from, d_to
        FROM public.loans;

        IF d_from IS NULL THEN
            d_from := date_trunc('month', now())::date;
            d_to := d_from;
        END IF;

        WHILE d_from <= d_to LOOP
                next_month := (d_from + INTERVAL '1 month')::date;
                part_name := format('loans_part_%s', to_char(d_from, 'YYYYMM'));

                EXECUTE format(
                        'CREATE TABLE IF NOT EXISTS %I PARTITION OF loans_part
                         FOR VALUES FROM (%L) TO (%L);',
                        part_name, d_from, next_month
                        );

                EXECUTE format(
                        'ALTER TABLE %I ADD CONSTRAINT %I_pkey PRIMARY KEY (id);',
                        part_name, part_name
                        );

                d_from := next_month;
            END LOOP;
    END;
$$;


INSERT INTO loans_part (id, book_copy_id, client_id, status, starts_at, ends_at)
SELECT id, book_copy_id, client_id, status, starts_at, ends_at
FROM public.loans;

ANALYZE loans_part;

EXPLAIN (ANALYZE, BUFFERS)
SELECT count(*) FROM loans_plain
WHERE starts_at = now() - INTERVAL '30 days';

EXPLAIN (ANALYZE, BUFFERS)
SELECT count(*) FROM loans_part
WHERE starts_at = now() - INTERVAL '30 days';

EXPLAIN (ANALYZE, BUFFERS)
SELECT *
FROM loans_plain
WHERE starts_at = now() - INTERVAL '90 days'
  AND client_id = 42
    LIMIT 100;

EXPLAIN (ANALYZE, BUFFERS)
SELECT *
FROM loans_part
WHERE starts_at = now() - INTERVAL '90 days'
  AND client_id = 42
    LIMIT 100;

EXPLAIN (ANALYZE, BUFFERS)
SELECT *
FROM loans_plain
WHERE client_id = 42
    LIMIT 1000;

EXPLAIN (ANALYZE, BUFFERS)
SELECT *
FROM loans_part
WHERE client_id = 42
    LIMIT 1000;

DROP TABLE IF EXISTS bench_logged, bench_unlogged;

CREATE TABLE bench_logged (
                              id bigserial PRIMARY KEY,
                              payload text,
                              created_at timestamp default now()
);

CREATE UNLOGGED TABLE bench_unlogged (
                                         id bigserial PRIMARY KEY,
                                         payload text,
                                         created_at timestamp default now()
);

EXPLAIN (ANALYZE, BUFFERS)
INSERT INTO bench_logged(payload)
SELECT md5(random()::text) FROM generate_series(1,100000);

EXPLAIN (ANALYZE, BUFFERS)
INSERT INTO bench_unlogged(payload)
SELECT md5(random()::text) FROM generate_series(1,100000);

EXPLAIN (ANALYZE, BUFFERS)
SELECT count(*) FROM bench_logged WHERE payload LIKE 'a%';

EXPLAIN (ANALYZE, BUFFERS)
SELECT count(*) FROM bench_unlogged WHERE payload LIKE 'a%';

SELECT count(*) FROM bench_logged;
SELECT count(*) FROM bench_unlogged;

EXPLAIN (ANALYZE, BUFFERS)
DELETE FROM bench_logged WHERE id <= 80000;

EXPLAIN (ANALYZE, BUFFERS)
DELETE FROM bench_unlogged WHERE id <= 80000;


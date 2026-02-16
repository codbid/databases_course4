// ============================================================
// Администрирование п.7: производительность — PROFILE и EXPLAIN
// ============================================================

// EXPLAIN — только план, запрос не выполняется
EXPLAIN
MATCH (c:Client)-[:BORROWED]->(copy:BookCopy)-[:INSTANCE_OF]->(b:Book)
WHERE b.genre = 'классика'
RETURN c.name, count(b) AS books
ORDER BY books DESC
LIMIT 5;

// PROFILE — выполнить и показать метрики (db hits, rows)
PROFILE
MATCH (c:Client)-[:BORROWED]->(copy:BookCopy)-[:INSTANCE_OF]->(b:Book)
WHERE b.genre = 'классика'
RETURN c.name, count(b) AS books
ORDER BY books DESC
LIMIT 5;

CREATE INDEX book_genre_idx IF NOT EXISTS
FOR (b:Book) ON (b.genre);

EXPLAIN
MATCH (c:Client)-[:BORROWED]->(copy:BookCopy)-[:INSTANCE_OF]->(b:Book)
WHERE b.genre = 'классика'
RETURN c.name, count(b) AS books
ORDER BY books DESC
LIMIT 5;

PROFILE
MATCH (b:Book)
WHERE b.genre = 'классика'
RETURN b;

PROFILE
MATCH (b:Book)
WHERE b.genre = 'классика'
RETURN b;


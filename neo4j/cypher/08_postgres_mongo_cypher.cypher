
// Аналог getBookWithAuthors
// В Mongo это match + два lookup + unwind + group
// В графе просто идём по связи WRITTEN_BY

MATCH (b:Book)-[:WRITTEN_BY]->(a:Author)
WHERE b.id = 'book1'
RETURN b.title AS title, b.isbn AS isbn, collect(a.name) AS authors;

// По сути в Cypher проще - связи уже есть, не нужно делать lookup и разворачивать массивы



// Аналог getTopAuthors
// В Mongo это authors + lookup + lookup + group + sort + limit
// Здесь просто считаем книги у каждого автора

MATCH (a:Author)<-[:WRITTEN_BY]-(b:Book)
WITH a, count(b) AS booksCount
ORDER BY booksCount DESC
LIMIT 10
RETURN a.name AS name, booksCount;

// Один обход по связи вместо нескольких lookup



// Аналог getLoansByPeriodGroupedByOffice
// В Postgres это JOIN loans, book_copies, offices + GROUP BY

MATCH (o:Office)<-[:LOCATED_AT]-(copy:BookCopy)<-[:BORROWED]-(c:Client)
MATCH (c)-[bor:BORROWED]->(copy)
WHERE bor.since >= date('2024-01-01') AND bor.since < date('2024-07-01')
WITH o, count(*) AS loans_count
ORDER BY loans_count DESC
RETURN o.id AS office_id, o.name AS office_name, loans_count;

// В графе просто читаем цепочку: офис - копия - выдача



// Аналог getOverdueActiveLoansGroupedByOffice
// В Postgres был бы LEFT JOIN и проверка на NULL

MATCH (o:Office)<-[:LOCATED_AT]-(copy:BookCopy)<-[:BORROWED]-(c:Client)
MATCH (c)-[bor:BORROWED]->(copy)
WHERE bor.returned = false AND bor.until < date()
WITH o, count(*) AS overdue_active
ORDER BY overdue_active DESC
RETURN o.id AS office_id, o.name AS office_name, overdue_active;

// Здесь всё проще признак "не возвращено" хранится прямо в ребре



// Аналог getAvailabilityByOffice
// В Mongo это match + lookup + group

MATCH (o:Office)<-[:LOCATED_AT]-(copy:BookCopy)
WHERE copy.status = 'AVAILABLE'
WITH o, count(copy) AS availableCopies
ORDER BY availableCopies DESC
RETURN o.id AS officeId, o.name AS officeName, availableCopies;

// Один MATCH вместо нескольких стадий агрегации

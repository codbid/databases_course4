// ============================================================
// 6. Сценарии Postgres/Mongo с join/lookup — переписаны на Cypher (MATCH)
// Описание: какой запрос проще/нагляднее
// ============================================================

// --- Эквивалент 1: getBookWithAuthors (MongoDB $lookup: book + book_authors + authors) ---
// Mongo: match book by _id, lookup("book_authors", "_id", "bookId", "links"), unwind, lookup("authors", "links.authorId", "_id", "author"), group.
// Cypher: один MATCH по графу — нагляднее, не нужны unwind и группировка.
MATCH (b:Book)-[:WRITTEN_BY]->(a:Author)
WHERE b.id = 'book1'
RETURN b.title AS title, b.isbn AS isbn, collect(a.name) AS authors;
// Вывод: Cypher проще — связи уже граф, один MATCH вместо двух $lookup + unwind + group.

// --- Эквивалент 2: getTopAuthors (Mongo: authors + lookup book_authors + lookup books, group by author, sum(1), sort, limit) ---
MATCH (a:Author)<-[:WRITTEN_BY]-(b:Book)
WITH a, count(b) AS booksCount
ORDER BY booksCount DESC
LIMIT 10
RETURN a.name AS name, booksCount;
// Вывод: в Cypher не нужны два lookup и unwind — один обход по связям.

// --- Эквивалент 3: getLoansByPeriodGroupedByOffice (Postgres: loans JOIN book_copies JOIN offices, GROUP BY office, COUNT) ---
MATCH (o:Office)<-[:LOCATED_AT]-(copy:BookCopy)<-[:BORROWED]-(c:Client)
MATCH (c)-[bor:BORROWED]->(copy)
WHERE bor.since >= date('2024-01-01') AND bor.since < date('2024-07-01')
WITH o, count(*) AS loans_count
ORDER BY loans_count DESC
RETURN o.id AS office_id, o.name AS office_name, loans_count;
// Вывод: в Postgres три таблицы и два JOIN по ключам; в Cypher те же сущности связаны рёбрами — запрос короче и читается как «офис — копии — выдачи».

// --- Эквивалент 4: getOverdueActiveLoansGroupedByOffice (Postgres: loans LEFT JOIN returns, JOIN copies, offices, WHERE return IS NULL AND ends_at < NOW) ---
MATCH (o:Office)<-[:LOCATED_AT]-(copy:BookCopy)<-[:BORROWED]-(c:Client)
MATCH (c)-[bor:BORROWED]->(copy)
WHERE bor.returned = false AND bor.until < date()
WITH o, count(*) AS overdue_active
ORDER BY overdue_active DESC
RETURN o.id AS office_id, o.name AS office_name, overdue_active;
// Вывод: в Postgres нужен LEFT JOIN returns и проверка на NULL; в нашей графовой модели «не возвращено» — свойство на ребре (returned = false), без отдельной таблицы returns — проще.

// --- Эквивалент 5: getAvailabilityByOffice (Mongo: book_copies match status=AVAILABLE, lookup offices, group by office) ---
MATCH (o:Office)<-[:LOCATED_AT]-(copy:BookCopy)
WHERE copy.status = 'AVAILABLE'
WITH o, count(copy) AS availableCopies
ORDER BY availableCopies DESC
RETURN o.id AS officeId, o.name AS officeName, availableCopies;
// Вывод: один MATCH вместо match + lookup + unwind + group.

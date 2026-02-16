// ============================================================
// 5. 5–6 агрегационных запросов (count, collect), ORDER BY, LIMIT
// ============================================================

// 1) Количество книг по жанрам, сортировка по убыванию количества, limit 5
MATCH (b:Book)
RETURN b.genre AS genre, count(b) AS cnt
ORDER BY cnt DESC
LIMIT 5;

// 2) Количество выданных копий по офисам (count), order by, limit
MATCH (o:Office)<-[:LOCATED_AT]-(copy:BookCopy)<-[:BORROWED]-(:Client)
RETURN o.name AS officeName, count(copy) AS loansCount
ORDER BY loansCount DESC
LIMIT 5;

// 3) collect: для каждого автора собрать список названий его книг, limit 5 авторов
MATCH (a:Author)<-[:WRITTEN_BY]-(b:Book)
WITH a, collect(b.title) AS books
RETURN a.name AS authorName, books
ORDER BY size(books) DESC
LIMIT 5;

// 4) Топ клиентов по числу выдач (count), сортировка, limit
MATCH (c:Client)-[:BORROWED]->()
RETURN c.name AS clientName, c.city AS city, count(*) AS borrowCount
ORDER BY borrowCount DESC
LIMIT 6;

// 5) Количество копий в каждом офисе (count), order by count desc, limit
MATCH (o:Office)<-[:LOCATED_AT]-(copy:BookCopy)
RETURN o.name AS officeName, count(copy) AS copiesCount
ORDER BY copiesCount DESC
LIMIT 5;

// 6) collect + count: по каждому офису — список id копий и их количество
MATCH (o:Office)<-[:LOCATED_AT]-(copy:BookCopy)
WITH o, collect(copy.id) AS copyIds, count(copy) AS total
RETURN o.name AS officeName, copyIds, total
ORDER BY total DESC
LIMIT 5;

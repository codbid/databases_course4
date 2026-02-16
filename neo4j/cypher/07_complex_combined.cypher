// ============================================================
// 8. Запрос, объединяющий цепочки, агрегации, фильтрацию
// Пример: «В каждом городе клиентов: топ-3 автора по числу взятых книг (через BORROWED->Copy->Book->Author), только классика»
// ============================================================

// Цепочка: Client - BORROWED -> BookCopy - INSTANCE_OF -> Book - WRITTEN_BY -> Author
// Фильтр: Book.genre = 'классика'
// Агрегация: count по (city, author), сортировка, limit по городу
MATCH (c:Client)-[:BORROWED]->(copy:BookCopy)-[:INSTANCE_OF]->(b:Book)-[:WRITTEN_BY]->(a:Author)
WHERE b.genre = 'классика'
WITH c.city AS city, a.name AS authorName, count(*) AS borrowCount
ORDER BY city, borrowCount DESC
WITH city, collect({author: authorName, count: borrowCount}) AS authorCounts
UNWIND range(0, size(authorCounts) - 1) AS idx
WITH city, authorCounts[idx] AS item, idx
WHERE idx < 3
RETURN city, item.author AS topAuthor, item.count AS borrowCount
ORDER BY city, borrowCount DESC;

// Альтернатива: топ-5 офисов по числу активных (не возвращённых) выдач, с именами клиентов (collect)
MATCH (o:Office)<-[:LOCATED_AT]-(copy:BookCopy)<-[:BORROWED]-(client:Client)
MATCH (client)-[bor:BORROWED]->(copy)
WHERE bor.returned = false
WITH o, count(*) AS activeLoans, collect(client.name) AS clientNames
ORDER BY activeLoans DESC
LIMIT 5
RETURN o.name AS officeName, activeLoans, clientNames;

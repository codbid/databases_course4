// Сложный запрос: для каждого города находим топ-3 авторов
// Считаем, сколько раз брали их книги жанра "классика"

MATCH (c:Client)-[:BORROWED]->(copy:BookCopy)-[:INSTANCE_OF]->(b:Book)-[:WRITTEN_BY]->(a:Author)
WHERE b.genre = 'классика'
WITH c.city AS city, a.name AS authorName, count(*) AS borrowCount
ORDER BY city, borrowCount DESC
WITH city, collect({author: authorName, count: borrowCount}) AS authorCounts
UNWIND range(0, size(authorCounts) - 1) AS idx
WITH city, authorCounts[idx] AS item, idx
WHERE idx < 3
RETURN city, item.author AS topAuthor, item.count AS borrowCount
ORDER BY borrowCount DESC;


// Альтернативный вариант: топ-5 офисов по количеству активных (не возвращённых) выдач
// Плюс собираем список клиентов, у которых сейчас книги на руках

MATCH (o:Office)<-[:LOCATED_AT]-(copy:BookCopy)<-[:BORROWED]-(client:Client)
MATCH (client)-[bor:BORROWED]->(copy)
WHERE bor.returned = false
WITH o, count(*) AS activeLoans, collect(client.name) AS clientNames
ORDER BY activeLoans DESC
LIMIT 5
RETURN o.name AS officeName, activeLoans, clientNames;

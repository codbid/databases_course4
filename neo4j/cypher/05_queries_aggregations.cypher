// Агрегационные запросы с count и collect, плюс сортировка и ограничение


// Считаем, сколько книг в каждом жанре, выводим топ-5
MATCH (b:Book)
RETURN b.genre AS genre, count(b) AS cnt
ORDER BY cnt DESC
LIMIT 5;


// Сколько всего выдач было в каждом офисе, тоже топ-5
MATCH (o:Office)<-[:LOCATED_AT]-(copy:BookCopy)<-[:BORROWED]-(:Client)
RETURN o.name AS officeName, count(copy) AS loansCount
ORDER BY loansCount DESC
LIMIT 5;


// Для каждого автора собираем список его книг, показываем 5 самых "плодовитых"
MATCH (a:Author)<-[:WRITTEN_BY]-(b:Book)
WITH a, collect(b.title) AS books
RETURN a.name AS authorName, books
ORDER BY size(books) DESC
LIMIT 5;


// Топ клиентов по количеству выдач
MATCH (c:Client)-[:BORROWED]->()
RETURN c.name AS clientName, c.city AS city, count(*) AS borrowCount
ORDER BY borrowCount DESC
LIMIT 6;


// Сколько копий хранится в каждом офисе, сортировка по убыванию
MATCH (o:Office)<-[:LOCATED_AT]-(copy:BookCopy)
RETURN o.name AS officeName, count(copy) AS copiesCount
ORDER BY copiesCount DESC
LIMIT 5;


// По каждому офису: список id копий и их общее количество
MATCH (o:Office)<-[:LOCATED_AT]-(copy:BookCopy)
WITH o, collect(copy.id) AS copyIds, count(copy) AS total
RETURN o.name AS officeName, copyIds, total
ORDER BY total DESC
LIMIT 5;

в каком филиале чаще всего забирают зарезервированные книги
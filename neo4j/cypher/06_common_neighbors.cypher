// ============================================================
// 7. Поиск общих соседей и сортировка по количеству общих связей
// «Общие соседи»: два узла связаны с одними и теми же узлами (например, две книги — одного автора; два клиента — брали одни копии)
// Сортировка по количеству общих связей (у кого больше общих соседей — выше)
// ============================================================

// Общие соседи для клиентов: клиенты, которые брали одни и те же копии (BookCopy). Считаем число общих копий и сортируем.
MATCH (c1:Client)-[:BORROWED]->(copy:BookCopy)<-[:BORROWED]-(c2:Client)
WHERE id(c1) < id(c2)
WITH c1, c2, count(copy) AS commonCopies
ORDER BY commonCopies DESC
RETURN c1.name AS client1, c2.name AS client2, commonCopies AS commonNeighborsCount
LIMIT 15;

// Общие соседи для книг: книги одного автора. Пары авторов с количеством общих книг (книг, написанных обоими) — в нашей модели у книги один автор, поэтому «общий сосед» — автор. Пары книг с общим автором и сортировка по «силе» связи.
MATCH (b1:Book)-[:WRITTEN_BY]->(a:Author)<-[:WRITTEN_BY]-(b2:Book)
WHERE id(b1) < id(b2)
WITH b1, b2, collect(a.name) AS commonAuthors, count(a) AS commonCount
ORDER BY commonCount DESC
RETURN b1.title AS book1, b2.title AS book2, commonAuthors, commonCount AS commonNeighborsCount
LIMIT 10;

// Клиенты с общими рекомендациями (RECOMMENDS): сколько общих книг рекомендуют пара клиентов
MATCH (c1:Client)-[:RECOMMENDS]->(b:Book)<-[:RECOMMENDS]-(c2:Client)
WHERE id(c1) < id(c2)
WITH c1, c2, count(b) AS commonRecommended
ORDER BY commonRecommended DESC
RETURN c1.name AS client1, c2.name AS client2, commonRecommended AS commonNeighborsCount
LIMIT 10;

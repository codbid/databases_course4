// Ищем пары клиентов, которые брали одни и те же копии книг
// Считаем, сколько у них общих копий, и сортируем по убыванию
MATCH (c1:Client)-[:BORROWED]->(copy:BookCopy)<-[:BORROWED]-(c2:Client)
WHERE id(c1) < id(c2)
WITH c1, c2, count(copy) AS commonCopies
ORDER BY commonCopies DESC
RETURN c1.name AS client1, c2.name AS client2, commonCopies AS commonNeighborsCount
LIMIT 15;


// Пары книг с общим автором
// В нашей модели у книги один автор, поэтому общий сосед это автор
MATCH (b1:Book)-[:WRITTEN_BY]->(a:Author)<-[:WRITTEN_BY]-(b2:Book)
WHERE id(b1) < id(b2)
WITH b1, b2, collect(a.name) AS commonAuthors, count(a) AS commonCount
ORDER BY commonCount DESC
RETURN b1.title AS book1, b2.title AS book2, commonAuthors, commonCount AS commonNeighborsCount
LIMIT 10;


// Пары клиентов, которые рекомендуют одни и те же книги
// Считаем, сколько у них совпадающих рекомендаций
MATCH (c1:Client)-[:RECOMMENDS]->(b:Book)<-[:RECOMMENDS]-(c2:Client)
WHERE id(c1) < id(c2)
WITH c1, c2, count(b) AS commonRecommended
ORDER BY commonRecommended DESC
RETURN c1.name AS client1, c2.name AS client2, commonRecommended AS commonNeighborsCount
LIMIT 10;

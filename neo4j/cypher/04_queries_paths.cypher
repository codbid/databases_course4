// ============================================================
// 4. Цепочки связей и переменная длина пути
// [:REL*2..4] — поиск по цепочке, косвенные связи. Минимум один запрос с *
// ============================================================

// 1) Клиенты, связанные через общие копии (один брал копию, другой тоже брал ту же копию)
// Путь: Client -[:BORROWED]-> BookCopy <-[:BORROWED]- Client (длина 2)
MATCH (c1:Client)-[:BORROWED]->(copy:BookCopy)<-[:BORROWED]-(c2:Client)
WHERE id(c1) < id(c2)
RETURN c1.name AS client1, c2.name AS client2, copy.id AS sharedCopyId
LIMIT 15;

// 2) Переменная длина пути: клиенты, связанные через 2–4 шага по BORROWED->Copy->Book<-Copy<-BORROWED
// «Клиенты, которые брали книги того же автора»: Client - BORROWED -> Copy - INSTANCE_OF -> Book - WRITTEN_BY -> Author <- WRITTEN_BY - Book <- INSTANCE_OF - Copy <- BORROWED - Client
MATCH path = (c1:Client)-[:BORROWED*1..1]->(:BookCopy)-[:INSTANCE_OF]->(b:Book)-[:WRITTEN_BY]->(a:Author)<-[:WRITTEN_BY]-(b2:Book)<-[:INSTANCE_OF]-(:BookCopy)<-[:BORROWED*1..1]-(c2:Client)
WHERE c1 <> c2 AND id(c1) < id(c2)
RETURN c1.name AS client1, c2.name AS client2, a.name AS commonAuthor
LIMIT 10;

// 3) Использование [*2..4]: путь произвольной длины 2–4 шага между клиентами (по любым связям)
MATCH path = (c1:Client)-[*2..4]-(c2:Client)
WHERE c1 <> c2 AND id(c1) < id(c2)
RETURN c1.name AS client1, c2.name AS client2, length(path) AS pathLength
ORDER BY pathLength
LIMIT 15;

// 4) Запрос с *: произвольная длина пути по RECOMMENDS (кто рекомендовал те же книги)
// Клиенты, связанные цепочкой рекомендаций одной и той же книги (путь длиной ровно 2: оба рекомендуют одну книгу)
MATCH (c1:Client)-[:RECOMMENDS]->(b:Book)<-[:RECOMMENDS]-(c2:Client)
WHERE c1 <> c2
RETURN c1.name AS client1, c2.name AS client2, b.title AS recommendedBook
LIMIT 15;

// 5) Переменная длина пути [*]: «книги, доступные через 1–3 шага от офиса» (офис -> копия -> книга; или офис -> копия -> книга -> автор)
MATCH path = (o:Office)<-[:LOCATED_AT]-(copy:BookCopy)-[:INSTANCE_OF*1..2]->(target)
WHERE o.name = 'Центральная библиотека'
  AND (target:Book OR target:Author)
RETURN o.name AS office, type(last(relationships(path))) AS relType, target.name AS targetName, target.title AS targetTitle
LIMIT 20;

// 6) Остановки, связанные через 2 маршрута — в нашей модели «офисы, связанные через 2 общие копии одной книги»
// Офисы, в которых есть копии одной и той же книги (связаны через общую книгу)
MATCH (o1:Office)<-[:LOCATED_AT]-(c1:BookCopy)-[:INSTANCE_OF]->(b:Book)<-[:INSTANCE_OF]-(c2:BookCopy)-[:LOCATED_AT]->(o2:Office)
WHERE o1 <> o2 AND id(o1) < id(o2)
RETURN o1.name AS office1, o2.name AS office2, b.title AS sharedBook;

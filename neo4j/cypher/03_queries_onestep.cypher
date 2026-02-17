// Книги жанра классика, которые вышли после 1860 года
MATCH (b:Book)
WHERE b.genre = 'классика' AND b.year > 1860
RETURN b.title AS title, b.year AS year
ORDER BY b.year;

// Клиенты из Москвы
MATCH (c:Client)
WHERE c.city = 'Москва'
RETURN c.name AS name, c.email AS email;

// Свободные копии книг в офисе "Центральная библиотека"
MATCH (copy:BookCopy)-[r:LOCATED_AT]->(o:Office)
WHERE copy.status = 'AVAILABLE' AND o.name = 'Центральная библиотека'
RETURN copy.id AS copyId, r.shelf AS shelf;

// Книги, которые написал Толстой Лев
MATCH (b:Book)-[:WRITTEN_BY]->(a:Author)
WHERE a.name = 'Толстой Лев'
RETURN b.title AS title, b.isbn AS isbn;

// Выданные книги, которые ещё не возвращены
MATCH (c:Client)-[bor:BORROWED]->(copy:BookCopy)
WHERE bor.returned = false
RETURN c.name AS clientName, copy.id AS copyId, bor.until AS dueDate;

// Резервации клиента с почтой ivan@mail.ru
MATCH (c:Client)-[res:RESERVED]->(copy:BookCopy)
WHERE c.email = 'ivan@mail.ru'
RETURN c.name AS client, copy.id AS copyId, res.from AS from, res.to AS to;

// Офисы, которые работают до 20:00 и позже
MATCH (o:Office)
WHERE o.working_time CONTAINS '20' OR o.working_time CONTAINS '21' OR o.working_time CONTAINS '22'
RETURN o.name AS name, o.working_time AS working_time;

// Авторы, родившиеся до 1850 года
MATCH (a:Author)
WHERE a.birthYear < 1850
RETURN a.name AS name, a.birthYear AS birthYear
ORDER BY a.birthYear;

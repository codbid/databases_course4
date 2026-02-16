// ============================================================
// 3. Не менее 6 запросов на 1 шаг. Фильтрация по связям и свойствам узла
// ============================================================

// 1) Книги жанра «классика», вышедшие после 1860
MATCH (b:Book)
WHERE b.genre = 'классика' AND b.year > 1860
RETURN b.title AS title, b.year AS year
ORDER BY b.year;

// 2) Клиенты из Москвы
MATCH (c:Client)
WHERE c.city = 'Москва'
RETURN c.name AS name, c.email AS email;

// 3) Копии в статусе AVAILABLE в офисе «Центральная библиотека»
MATCH (copy:BookCopy)-[r:LOCATED_AT]->(o:Office)
WHERE copy.status = 'AVAILABLE' AND o.name = 'Центральная библиотека'
RETURN copy.id AS copyId, r.shelf AS shelf;

// 4) Книги, написанные автором «Толстой Лев»
MATCH (b:Book)-[:WRITTEN_BY]->(a:Author)
WHERE a.name = 'Толстой Лев'
RETURN b.title AS title, b.isbn AS isbn;

// 5) Выдачи (BORROWED), ещё не возвращённые (returned = false)
MATCH (c:Client)-[bor:BORROWED]->(copy:BookCopy)
WHERE bor.returned = false
RETURN c.name AS clientName, copy.id AS copyId, bor.until AS dueDate;

// 6) Резервации клиента с email ivan@mail.ru
MATCH (c:Client)-[res:RESERVED]->(copy:BookCopy)
WHERE c.email = 'ivan@mail.ru'
RETURN c.name AS client, copy.id AS copyId, res.from AS from, res.to AS to;

// 7) Офисы с рабочим временем до 20:00 (фильтр по свойству)
MATCH (o:Office)
WHERE o.working_time CONTAINS '20' OR o.working_time CONTAINS '21' OR o.working_time CONTAINS '22'
RETURN o.name AS name, o.working_time AS working_time;

// 8) Авторы, родившиеся до 1850
MATCH (a:Author)
WHERE a.birthYear < 1850
RETURN a.name AS name, a.birthYear AS birthYear
ORDER BY a.birthYear;

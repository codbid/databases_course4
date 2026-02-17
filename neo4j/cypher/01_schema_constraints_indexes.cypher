// Настраиваем ограничения и индексы

// У клиента email должен быть уникальным
CREATE CONSTRAINT client_email_unique IF NOT EXISTS
FOR (c:Client) REQUIRE c.email IS UNIQUE;

// У книги ISBN уникальный
CREATE CONSTRAINT book_isbn_unique IF NOT EXISTS
FOR (b:Book) REQUIRE b.isbn IS UNIQUE;

// Имя автора тоже делаем уникальным
CREATE CONSTRAINT author_name_unique IF NOT EXISTS
FOR (a:Author) REQUIRE a.name IS UNIQUE;


// В Enterprise можно было бы добавить обязательные поля (как NOT NULL в SQL)
// CREATE CONSTRAINT client_name_exists IF NOT EXISTS FOR (c:Client) REQUIRE c.name IS NOT NULL;
// CREATE CONSTRAINT book_title_exists IF NOT EXISTS FOR (b:Book) REQUIRE b.title IS NOT NULL;


// Индексы для ускорения поиска

// По городу клиента
CREATE INDEX client_city_idx IF NOT EXISTS
FOR (c:Client) ON (c.city);

// По жанру книги
CREATE INDEX book_genre_idx IF NOT EXISTS
FOR (b:Book) ON (b.genre);

// По году издания
CREATE INDEX book_year_idx IF NOT EXISTS
FOR (b:Book) ON (b.year);

// По названию офиса
CREATE INDEX office_name_idx IF NOT EXISTS
FOR (o:Office) ON (o.name);

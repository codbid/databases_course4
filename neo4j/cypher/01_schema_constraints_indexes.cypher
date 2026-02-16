// ============================================================
// 1. Ограничения целостности (constraints) и индексы
// Администрирование п.4: уникальность, существование, индексы
// ============================================================

// --- Ограничение уникальности ---
CREATE CONSTRAINT client_email_unique IF NOT EXISTS
FOR (c:Client) REQUIRE c.email IS UNIQUE;

CREATE CONSTRAINT book_isbn_unique IF NOT EXISTS
FOR (b:Book) REQUIRE b.isbn IS UNIQUE;

CREATE CONSTRAINT author_name_unique IF NOT EXISTS
FOR (a:Author) REQUIRE a.name IS UNIQUE;

// --- Ограничение существования (NOT NULL) — только Neo4j Enterprise ---
// CREATE CONSTRAINT client_name_exists IF NOT EXISTS FOR (c:Client) REQUIRE c.name IS NOT NULL;
// CREATE CONSTRAINT book_title_exists IF NOT EXISTS FOR (b:Book) REQUIRE b.title IS NOT NULL;

// --- Индексы (для ускорения фильтрации по свойствам) ---
CREATE INDEX client_city_idx IF NOT EXISTS
FOR (c:Client) ON (c.city);

CREATE INDEX book_genre_idx IF NOT EXISTS
FOR (b:Book) ON (b.genre);

CREATE INDEX book_year_idx IF NOT EXISTS
FOR (b:Book) ON (b.year);

CREATE INDEX office_name_idx IF NOT EXISTS
FOR (o:Office) ON (o.name);

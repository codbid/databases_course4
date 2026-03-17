// ============================================================
// Администрирование п.2: пользователи reader, publisher
// Выполнять под администратором neo4j (в Neo4j Browser или cypher-shell).
// Роли reader/publisher работают только в Neo4j Enterprise.
// ============================================================

// Создание пользователей (Community и Enterprise)
CREATE USER reader SET PASSWORD 'reader123' CHANGE NOT REQUIRED;
CREATE USER publisher SET PASSWORD 'publisher123' CHANGE NOT REQUIRED;

// ---- Только Neo4j Enterprise: выдать роли ----
// GRANT ROLE reader TO reader;
// GRANT ROLE publisher TO publisher;

// Проверка:
// 1) Подключиться как reader → выполнить CREATE (n:Test {id:1}) → в Enterprise: ошибка доступа.
// 2) Подключиться как publisher → выполнить MATCH (n) RETURN count(n) → чтение разрешено.

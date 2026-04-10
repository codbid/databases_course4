// ============================================================
// Администрирование п.2: пользователи reader, publisher
// В Neo4j Community нет кастомных ролей reader/publisher — только admin, public.
// В Enterprise: создать пользователей и выдать роли.
// Выполнять под пользователем neo4j (admin).
// ============================================================

// Создание пользователей (работает в Community)
CREATE USER reader SET PASSWORD 'reader123' CHANGE NOT REQUIRED;
CREATE USER publisher SET PASSWORD 'publisher123' CHANGE NOT REQUIRED;

// В Enterprise дополнительно (если есть роли reader/publisher):
// GRANT ROLE reader TO reader;
// GRANT ROLE publisher TO publisher;

// Проверка: под пользователем reader попытка записи должна дать ошибку (если роль настроена).
// Под publisher — чтение разрешено.

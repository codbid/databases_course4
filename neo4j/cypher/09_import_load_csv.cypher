// ============================================================
// Импорт через LOAD CSV (п.3 Администрирование)
// Файл из каталога import: file:///offices_import.csv
// ============================================================

LOAD CSV WITH HEADERS FROM 'file:///offices_import.csv' AS row
MERGE (o:Office {id: row.id})
SET o.name = row.name, o.address = row.address, o.working_time = row.working_time
RETURN count(o) AS imported;

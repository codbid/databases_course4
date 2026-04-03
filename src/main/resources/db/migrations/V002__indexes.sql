-- book_copies
-- часто фильтруемся по статусу и офису
-- (агрегации доступных экземпляров)
CREATE INDEX idx_book_copies_office_status
    ON book_copies (office_id, status);

-- join book_copies - book_link
-- используется в статистике по книгам
CREATE INDEX idx_book_copies_book_link
    ON book_copies (book_link_id);

-- выборка займов за период
-- используется в аналитике по офисам
CREATE INDEX idx_loans_starts_at
    ON loans (starts_at);

-- join loans - book_copies
CREATE INDEX idx_loans_book_copy
    ON loans (book_copy_id);

-- группировка активных займов по клиентам
CREATE INDEX idx_loans_client
    ON loans (client_id);

-- просроченные займы (endы_at < now())
CREATE INDEX idx_loans_ends_at
    ON loans (ends_at);

-- проверка, возвращён ли займ
-- используется как anti-join (IS NULL)
CREATE INDEX idx_returns_loan
    ON returns (loan_id);

-- агрегации и рейтинги книг
CREATE INDEX idx_book_link_id
    ON book_link (id);

-- связь бронирований с экземплярами книг
CREATE INDEX idx_reservations_book_copy
    ON reservations (book_copy_id);

-- бронирования по клиенту
CREATE INDEX idx_reservations_client
    ON reservations (client_id);

-- поиск штрафов по займам
CREATE INDEX idx_fines_loan
    ON fines (loan_id);

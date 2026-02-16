// ============================================================
// 2. Данные: минимум 3 типа узлов, 2 типа связей
// 30-50 узлов, 100+ связей. MERGE, свойства на узлах и рёбрах
// Узлы: Client, Book, Author, Office, BookCopy
// Связи: BORROWED, WRITTEN_BY, LOCATED_AT, RESERVED, RECOMMENDS
// ============================================================

// ---------- Офисы (5) ----------
MERGE (o1:Office {id: 'off1', name: 'Центральная библиотека', address: 'ул. Ленина 1', working_time: '9-21'})
MERGE (o2:Office {id: 'off2', name: 'Филиал Север', address: 'ул. Мира 10', working_time: '10-20'})
MERGE (o3:Office {id: 'off3', name: 'Филиал Юг', address: 'пр. Победы 5', working_time: '9-18'})
MERGE (o4:Office {id: 'off4', name: 'Детская библиотека', address: 'ул. Гагарина 3', working_time: '10-19'})
MERGE (o5:Office {id: 'off5', name: 'Научная библиотека', address: 'ул. Науки 7', working_time: '8-22'});

// ---------- Авторы (10) ----------
MERGE (a1:Author {id: 'auth1', name: 'Толстой Лев', birthYear: 1828})
MERGE (a2:Author {id: 'auth2', name: 'Достоевский Фёдор', birthYear: 1821})
MERGE (a3:Author {id: 'auth3', name: 'Пушкин Александр', birthYear: 1799})
MERGE (a4:Author {id: 'auth4', name: 'Гоголь Николай', birthYear: 1809})
MERGE (a5:Author {id: 'auth5', name: 'Чехов Антон', birthYear: 1860})
MERGE (a6:Author {id: 'auth6', name: 'Оруэлл Джордж', birthYear: 1903})
MERGE (a7:Author {id: 'auth7', name: 'Стивен Кинг', birthYear: 1947})
MERGE (a8:Author {id: 'auth8', name: 'Роулинг Джоан', birthYear: 1965})
MERGE (a9:Author {id: 'auth9', name: 'Азимов Айзек', birthYear: 1920})
MERGE (a10:Author {id: 'auth10', name: 'Лем Станислав', birthYear: 1921});

// ---------- Книги (15) ----------
MERGE (b1:Book {id: 'book1', isbn: '978-5-001-001', title: 'Война и мир', genre: 'классика', year: 1869})
MERGE (b2:Book {id: 'book2', isbn: '978-5-001-002', title: 'Анна Каренина', genre: 'классика', year: 1877})
MERGE (b3:Book {id: 'book3', isbn: '978-5-001-003', title: 'Преступление и наказание', genre: 'классика', year: 1866})
MERGE (b4:Book {id: 'book4', isbn: '978-5-001-004', title: 'Евгений Онегин', genre: 'поэзия', year: 1833})
MERGE (b5:Book {id: 'book5', isbn: '978-5-001-005', title: 'Мёртвые души', genre: 'классика', year: 1842})
MERGE (b6:Book {id: 'book6', isbn: '978-5-001-006', title: 'Вишнёвый сад', genre: 'драма', year: 1904})
MERGE (b7:Book {id: 'book7', isbn: '978-5-001-007', title: '1984', genre: 'антиутопия', year: 1949})
MERGE (b8:Book {id: 'book8', isbn: '978-5-001-008', title: 'Сияние', genre: 'ужасы', year: 1977})
MERGE (b9:Book {id: 'book9', isbn: '978-5-001-009', title: 'Гарри Поттер и философский камень', genre: 'фэнтези', year: 1997})
MERGE (b10:Book {id: 'book10', isbn: '978-5-001-010', title: 'Я, робот', genre: 'фантастика', year: 1950})
MERGE (b11:Book {id: 'book11', isbn: '978-5-001-011', title: 'Солярис', genre: 'фантастика', year: 1961})
MERGE (b12:Book {id: 'book12', isbn: '978-5-001-012', title: 'Идиот', genre: 'классика', year: 1869})
MERGE (b13:Book {id: 'book13', isbn: '978-5-001-013', title: 'Ревизор', genre: 'драма', year: 1836})
MERGE (b14:Book {id: 'book14', isbn: '978-5-001-014', title: 'Скотный двор', genre: 'антиутопия', year: 1945})
MERGE (b15:Book {id: 'book15', isbn: '978-5-001-015', title: 'Основание', genre: 'фантастика', year: 1951});

// ---------- Связи WRITTEN_BY (Book -> Author), свойства на рёбрах ----------
MERGE (b1)-[:WRITTEN_BY {since: 1869, primary: true}]->(a1)
MERGE (b2)-[:WRITTEN_BY {since: 1877, primary: true}]->(a1)
MERGE (b3)-[:WRITTEN_BY {since: 1866, primary: true}]->(a2)
MERGE (b12)-[:WRITTEN_BY {since: 1869, primary: true}]->(a2)
MERGE (b4)-[:WRITTEN_BY {since: 1833, primary: true}]->(a3)
MERGE (b5)-[:WRITTEN_BY {since: 1842, primary: true}]->(a4)
MERGE (b13)-[:WRITTEN_BY {since: 1836, primary: true}]->(a4)
MERGE (b6)-[:WRITTEN_BY {since: 1904, primary: true}]->(a5)
MERGE (b7)-[:WRITTEN_BY {since: 1949, primary: true}]->(a6)
MERGE (b14)-[:WRITTEN_BY {since: 1945, primary: true}]->(a6)
MERGE (b8)-[:WRITTEN_BY {since: 1977, primary: true}]->(a7)
MERGE (b9)-[:WRITTEN_BY {since: 1997, primary: true}]->(a8)
MERGE (b10)-[:WRITTEN_BY {since: 1950, primary: true}]->(a9)
MERGE (b15)-[:WRITTEN_BY {since: 1951, primary: true}]->(a9)
MERGE (b11)-[:WRITTEN_BY {since: 1961, primary: true}]->(a10);

// ---------- Копии книг (BookCopy): каждая книга может иметь копии в офисах ----------
MERGE (c1:BookCopy {id: 'copy1', status: 'AVAILABLE', acquiredAt: date('2020-01-15')})
MERGE (c2:BookCopy {id: 'copy2', status: 'BORROWED', acquiredAt: date('2019-06-01')})
MERGE (c3:BookCopy {id: 'copy3', status: 'AVAILABLE', acquiredAt: date('2021-03-10')})
MERGE (c4:BookCopy {id: 'copy4', status: 'RESERVED', acquiredAt: date('2020-11-20')})
MERGE (c5:BookCopy {id: 'copy5', status: 'AVAILABLE', acquiredAt: date('2018-09-05')})
MERGE (c6:BookCopy {id: 'copy6', status: 'BORROWED', acquiredAt: date('2022-02-14')})
MERGE (c7:BookCopy {id: 'copy7', status: 'AVAILABLE', acquiredAt: date('2021-07-22')})
MERGE (c8:BookCopy {id: 'copy8', status: 'BORROWED', acquiredAt: date('2019-12-01')})
MERGE (c9:BookCopy {id: 'copy9', status: 'AVAILABLE', acquiredAt: date('2020-05-18')})
MERGE (c10:BookCopy {id: 'copy10', status: 'AVAILABLE', acquiredAt: date('2022-01-03')})
MERGE (c11:BookCopy {id: 'copy11', status: 'BORROWED', acquiredAt: date('2021-08-11')})
MERGE (c12:BookCopy {id: 'copy12', status: 'AVAILABLE', acquiredAt: date('2019-04-25')})
MERGE (c13:BookCopy {id: 'copy13', status: 'RESERVED', acquiredAt: date('2020-10-30')})
MERGE (c14:BookCopy {id: 'copy14', status: 'AVAILABLE', acquiredAt: date('2022-06-07')})
MERGE (c15:BookCopy {id: 'copy15', status: 'BORROWED', acquiredAt: date('2018-03-12')})
MERGE (c16:BookCopy {id: 'copy16', status: 'AVAILABLE', acquiredAt: date('2021-11-09')})
MERGE (c17:BookCopy {id: 'copy17', status: 'AVAILABLE', acquiredAt: date('2019-07-16')})
MERGE (c18:BookCopy {id: 'copy18', status: 'BORROWED', acquiredAt: date('2020-02-28')})
MERGE (c19:BookCopy {id: 'copy19', status: 'AVAILABLE', acquiredAt: date('2022-04-01')})
MERGE (c20:BookCopy {id: 'copy20', status: 'AVAILABLE', acquiredAt: date('2021-09-14')});

// Связь копии с книгой (экземпляр какой книги)
MERGE (c1)-[:INSTANCE_OF]->(b1) MERGE (c2)-[:INSTANCE_OF]->(b1) MERGE (c3)-[:INSTANCE_OF]->(b2)
MERGE (c4)-[:INSTANCE_OF]->(b3) MERGE (c5)-[:INSTANCE_OF]->(b4) MERGE (c6)-[:INSTANCE_OF]->(b5)
MERGE (c7)-[:INSTANCE_OF]->(b6) MERGE (c8)-[:INSTANCE_OF]->(b7) MERGE (c9)-[:INSTANCE_OF]->(b8)
MERGE (c10)-[:INSTANCE_OF]->(b9) MERGE (c11)-[:INSTANCE_OF]->(b10) MERGE (c12)-[:INSTANCE_OF]->(b11)
MERGE (c13)-[:INSTANCE_OF]->(b12) MERGE (c14)-[:INSTANCE_OF]->(b13) MERGE (c15)-[:INSTANCE_OF]->(b14)
MERGE (c16)-[:INSTANCE_OF]->(b15) MERGE (c17)-[:INSTANCE_OF]->(b1) MERGE (c18)-[:INSTANCE_OF]->(b3)
MERGE (c19)-[:INSTANCE_OF]->(b7) MERGE (c20)-[:INSTANCE_OF]->(b9);

// LOCATED_AT: копия в офисе (свойство floor на ребре)
MERGE (c1)-[:LOCATED_AT {floor: 1, shelf: 'A1'}]->(o1) MERGE (c2)-[:LOCATED_AT {floor: 1, shelf: 'A2'}]->(o1)
MERGE (c3)-[:LOCATED_AT {floor: 2, shelf: 'B1'}]->(o1) MERGE (c4)-[:LOCATED_AT {floor: 1, shelf: 'A3'}]->(o2)
MERGE (c5)-[:LOCATED_AT {floor: 1, shelf: 'C1'}]->(o2) MERGE (c6)-[:LOCATED_AT {floor: 2, shelf: 'B2'}]->(o2)
MERGE (c7)-[:LOCATED_AT {floor: 1, shelf: 'A4'}]->(o3) MERGE (c8)-[:LOCATED_AT {floor: 1, shelf: 'D1'}]->(o3)
MERGE (c9)-[:LOCATED_AT {floor: 2, shelf: 'E1'}]->(o3) MERGE (c10)-[:LOCATED_AT {floor: 1, shelf: 'F1'}]->(o4)
MERGE (c11)-[:LOCATED_AT {floor: 1, shelf: 'A5'}]->(o4) MERGE (c12)-[:LOCATED_AT {floor: 3, shelf: 'G1'}]->(o5)
MERGE (c13)-[:LOCATED_AT {floor: 2, shelf: 'B3'}]->(o5) MERGE (c14)-[:LOCATED_AT {floor: 1, shelf: 'A6'}]->(o1)
MERGE (c15)-[:LOCATED_AT {floor: 2, shelf: 'H1'}]->(o2) MERGE (c16)-[:LOCATED_AT {floor: 1, shelf: 'A7'}]->(o5)
MERGE (c17)-[:LOCATED_AT {floor: 1, shelf: 'A8'}]->(o1) MERGE (c18)-[:LOCATED_AT {floor: 2, shelf: 'B4'}]->(o3)
MERGE (c19)-[:LOCATED_AT {floor: 1, shelf: 'D2'}]->(o2) MERGE (c20)-[:LOCATED_AT {floor: 1, shelf: 'F2'}]->(o4);

// ---------- Клиенты (12) ----------
MERGE (cl1:Client {id: 'cl1', name: 'Иван Петров', email: 'ivan@mail.ru', city: 'Москва'})
MERGE (cl2:Client {id: 'cl2', name: 'Мария Сидорова', email: 'maria@gmail.com', city: 'Москва'})
MERGE (cl3:Client {id: 'cl3', name: 'Алексей Козлов', email: 'alex@yandex.ru', city: 'Санкт-Петербург'})
MERGE (cl4:Client {id: 'cl4', name: 'Елена Новикова', email: 'elena@mail.ru', city: 'Казань'})
MERGE (cl5:Client {id: 'cl5', name: 'Дмитрий Волков', email: 'dmitry@gmail.com', city: 'Москва'})
MERGE (cl6:Client {id: 'cl6', name: 'Ольга Морозова', email: 'olga@yandex.ru', city: 'Новосибирск'})
MERGE (cl7:Client {id: 'cl7', name: 'Сергей Соколов', email: 'sergey@mail.ru', city: 'Екатеринбург'})
MERGE (cl8:Client {id: 'cl8', name: 'Анна Лебедева', email: 'anna@gmail.com', city: 'Москва'})
MERGE (cl9:Client {id: 'cl9', name: 'Николай Кузнецов', email: 'nikolay@yandex.ru', city: 'Санкт-Петербург'})
MERGE (cl10:Client {id: 'cl10', name: 'Татьяна Попова', email: 'tatyana@mail.ru', city: 'Казань'})
MERGE (cl11:Client {id: 'cl11', name: 'Павел Фёдоров', email: 'pavel@gmail.com', city: 'Москва'})
MERGE (cl12:Client {id: 'cl12', name: 'Наталья Михайлова', email: 'natalya@yandex.ru', city: 'Воронеж'});

// ---------- BORROWED: клиент брал копию (свойства since, until, returned на ребре) — много связей ----------
MERGE (cl1)-[:BORROWED {since: date('2024-01-10'), until: date('2024-02-10'), returned: true}]->(c2)
MERGE (cl1)-[:BORROWED {since: date('2024-03-01'), until: date('2024-04-01'), returned: true}]->(c6)
MERGE (cl1)-[:BORROWED {since: date('2024-05-15'), until: date('2024-06-15'), returned: false}]->(c8)
MERGE (cl2)-[:BORROWED {since: date('2024-02-01'), until: date('2024-03-01'), returned: true}]->(c3)
MERGE (cl2)-[:BORROWED {since: date('2024-04-10'), until: date('2024-05-10'), returned: true}]->(c10)
MERGE (cl2)-[:BORROWED {since: date('2024-06-01'), until: date('2024-07-01'), returned: false}]->(c11)
MERGE (cl3)-[:BORROWED {since: date('2023-11-01'), until: date('2023-12-01'), returned: true}]->(c1)
MERGE (cl3)-[:BORROWED {since: date('2024-01-20'), until: date('2024-02-20'), returned: true}]->(c5)
MERGE (cl3)-[:BORROWED {since: date('2024-05-01'), until: date('2024-06-01'), returned: true}]->(c7)
MERGE (cl3)-[:BORROWED {since: date('2024-07-01'), until: date('2024-08-01'), returned: false}]->(c15)
MERGE (cl4)-[:BORROWED {since: date('2024-03-15'), until: date('2024-04-15'), returned: true}]->(c4)
MERGE (cl4)-[:BORROWED {since: date('2024-06-10'), until: date('2024-07-10'), returned: false}]->(c13)
MERGE (cl5)-[:BORROWED {since: date('2024-02-20'), until: date('2024-03-20'), returned: true}]->(c9)
MERGE (cl5)-[:BORROWED {since: date('2024-04-01'), until: date('2024-05-01'), returned: true}]->(c12)
MERGE (cl5)-[:BORROWED {since: date('2024-08-01'), until: date('2024-09-01'), returned: false}]->(c18)
MERGE (cl6)-[:BORROWED {since: date('2023-12-10'), until: date('2024-01-10'), returned: true}]->(c14)
MERGE (cl6)-[:BORROWED {since: date('2024-05-20'), until: date('2024-06-20'), returned: true}]->(c16)
MERGE (cl7)-[:BORROWED {since: date('2024-01-05'), until: date('2024-02-05'), returned: true}]->(c17)
MERGE (cl7)-[:BORROWED {since: date('2024-06-15'), until: date('2024-07-15'), returned: false}]->(c19)
MERGE (cl8)-[:BORROWED {since: date('2024-03-10'), until: date('2024-04-10'), returned: true}]->(c1)
MERGE (cl8)-[:BORROWED {since: date('2024-07-01'), until: date('2024-08-01'), returned: false}]->(c2)
MERGE (cl9)-[:BORROWED {since: date('2024-02-15'), until: date('2024-03-15'), returned: true}]->(c5)
MERGE (cl9)-[:BORROWED {since: date('2024-05-01'), until: date('2024-06-01'), returned: true}]->(c10)
MERGE (cl9)-[:BORROWED {since: date('2024-09-01'), until: date('2024-10-01'), returned: false}]->(c6)
MERGE (cl10)-[:BORROWED {since: date('2024-04-20'), until: date('2024-05-20'), returned: true}]->(c7)
MERGE (cl10)-[:BORROWED {since: date('2024-08-10'), until: date('2024-09-10'), returned: false}]->(c11)
MERGE (cl11)-[:BORROWED {since: date('2024-01-01'), until: date('2024-02-01'), returned: true}]->(c3)
MERGE (cl11)-[:BORROWED {since: date('2024-06-01'), until: date('2024-07-01'), returned: true}]->(c14)
MERGE (cl11)-[:BORROWED {since: date('2024-09-15'), until: date('2024-10-15'), returned: false}]->(c8)
MERGE (cl12)-[:BORROWED {since: date('2024-03-01'), until: date('2024-04-01'), returned: true}]->(c16)
MERGE (cl12)-[:BORROWED {since: date('2024-07-20'), until: date('2024-08-20'), returned: false}]->(c4)
MERGE (cl1)-[:BORROWED {since: date('2023-10-01'), until: date('2023-11-01'), returned: true}]->(c17)
MERGE (cl2)-[:BORROWED {since: date('2023-09-15'), until: date('2023-10-15'), returned: true}]->(c14)
MERGE (cl3)-[:BORROWED {since: date('2024-04-01'), until: date('2024-05-01'), returned: true}]->(c16)
MERGE (cl4)-[:BORROWED {since: date('2024-01-01'), until: date('2024-02-01'), returned: true}]->(c5)
MERGE (cl5)-[:BORROWED {since: date('2024-07-10'), until: date('2024-08-10'), returned: true}]->(c1)
MERGE (cl6)-[:BORROWED {since: date('2024-03-01'), until: date('2024-04-01'), returned: true}]->(c9)
MERGE (cl7)-[:BORROWED {since: date('2024-04-15'), until: date('2024-05-15'), returned: true}]->(c12)
MERGE (cl8)-[:BORROWED {since: date('2024-05-20'), until: date('2024-06-20'), returned: true}]->(c3)
MERGE (cl9)-[:BORROWED {since: date('2024-06-10'), until: date('2024-07-10'), returned: true}]->(c14)
MERGE (cl10)-[:BORROWED {since: date('2024-02-01'), until: date('2024-03-01'), returned: true}]->(c16)
MERGE (cl11)-[:BORROWED {since: date('2024-08-01'), until: date('2024-09-01'), returned: true}]->(c17)
MERGE (cl12)-[:BORROWED {since: date('2024-05-01'), until: date('2024-06-01'), returned: true}]->(c7);

// ---------- RESERVED: резервации (свойства на ребре) ----------
MERGE (cl1)-[:RESERVED {from: date('2024-09-01'), to: date('2024-09-15')}]->(c4)
MERGE (cl2)-[:RESERVED {from: date('2024-08-05'), to: date('2024-08-20')}]->(c13)
MERGE (cl4)-[:RESERVED {from: date('2024-10-01'), to: date('2024-10-15')}]->(c9)
MERGE (cl5)-[:RESERVED {from: date('2024-09-10'), to: date('2024-09-25')}]->(c1)
MERGE (cl8)-[:RESERVED {from: date('2024-08-15'), to: date('2024-08-30')}]->(c12);

// ---------- RECOMMENDS: клиент рекомендует книгу (для графа «общие интересы») ----------
MERGE (cl1)-[:RECOMMENDS {rating: 5, since: date('2024-01-15')}]->(b1)
MERGE (cl1)-[:RECOMMENDS {rating: 4, since: date('2024-03-20')}]->(b7)
MERGE (cl2)-[:RECOMMENDS {rating: 5, since: date('2024-02-10')}]->(b9)
MERGE (cl2)-[:RECOMMENDS {rating: 4, since: date('2024-04-05')}]->(b1)
MERGE (cl3)-[:RECOMMENDS {rating: 5, since: date('2024-01-25')}]->(b11)
MERGE (cl3)-[:RECOMMENDS {rating: 4, since: date('2024-05-12')}]->(b7)
MERGE (cl4)-[:RECOMMENDS {rating: 5, since: date('2024-03-01')}]->(b3)
MERGE (cl5)-[:RECOMMENDS {rating: 4, since: date('2024-04-18')}]->(b10)
MERGE (cl5)-[:RECOMMENDS {rating: 5, since: date('2024-06-01')}]->(b15)
MERGE (cl6)-[:RECOMMENDS {rating: 5, since: date('2024-02-20')}]->(b9)
MERGE (cl7)-[:RECOMMENDS {rating: 4, since: date('2024-05-05')}]->(b8)
MERGE (cl8)-[:RECOMMENDS {rating: 5, since: date('2024-01-10')}]->(b1)
MERGE (cl8)-[:RECOMMENDS {rating: 4, since: date('2024-06-15')}]->(b9)
MERGE (cl9)-[:RECOMMENDS {rating: 5, since: date('2024-03-15')}]->(b7)
MERGE (cl10)-[:RECOMMENDS {rating: 4, since: date('2024-07-01')}]->(b6)
MERGE (cl11)-[:RECOMMENDS {rating: 5, since: date('2024-04-01')}]->(b11)
MERGE (cl12)-[:RECOMMENDS {rating: 4, since: date('2024-08-01')}]->(b15);

MATCH (n) DETACH DELETE n;

MERGE (:Office {id:'off1',name:'Центральная библиотека',address:'ул. Ленина 1',working_time:'9-21'})
MERGE (:Office {id:'off2',name:'Филиал Север',address:'ул. Мира 10',working_time:'10-20'})
MERGE (:Office {id:'off3',name:'Филиал Юг',address:'пр. Победы 5',working_time:'9-18'})
MERGE (:Office {id:'off4',name:'Детская библиотека',address:'ул. Гагарина 3',working_time:'10-19'})
MERGE (:Office {id:'off5',name:'Научная библиотека',address:'ул. Науки 7',working_time:'8-22'});

MERGE (:Author {id:'auth1',name:'Толстой Лев',birthYear:1828})
MERGE (:Author {id:'auth2',name:'Достоевский Фёдор',birthYear:1821})
MERGE (:Author {id:'auth3',name:'Пушкин Александр',birthYear:1799})
MERGE (:Author {id:'auth4',name:'Гоголь Николай',birthYear:1809})
MERGE (:Author {id:'auth5',name:'Чехов Антон',birthYear:1860})
MERGE (:Author {id:'auth6',name:'Оруэлл Джордж',birthYear:1903})
MERGE (:Author {id:'auth7',name:'Стивен Кинг',birthYear:1947})
MERGE (:Author {id:'auth8',name:'Роулинг Джоан',birthYear:1965})
MERGE (:Author {id:'auth9',name:'Азимов Айзек',birthYear:1920})
MERGE (:Author {id:'auth10',name:'Лем Станислав',birthYear:1921});

MERGE (:Book {id:'book1',isbn:'978-5-001-001',title:'Война и мир',genre:'классика',year:1869})
MERGE (:Book {id:'book2',isbn:'978-5-001-002',title:'Анна Каренина',genre:'классика',year:1877})
MERGE (:Book {id:'book3',isbn:'978-5-001-003',title:'Преступление и наказание',genre:'классика',year:1866})
MERGE (:Book {id:'book4',isbn:'978-5-001-004',title:'Евгений Онегин',genre:'поэзия',year:1833})
MERGE (:Book {id:'book5',isbn:'978-5-001-005',title:'Мёртвые души',genre:'классика',year:1842})
MERGE (:Book {id:'book6',isbn:'978-5-001-006',title:'Вишнёвый сад',genre:'драма',year:1904})
MERGE (:Book {id:'book7',isbn:'978-5-001-007',title:'1984',genre:'антиутопия',year:1949})
MERGE (:Book {id:'book8',isbn:'978-5-001-008',title:'Сияние',genre:'ужасы',year:1977})
MERGE (:Book {id:'book9',isbn:'978-5-001-009',title:'Гарри Поттер и философский камень',genre:'фэнтези',year:1997})
MERGE (:Book {id:'book10',isbn:'978-5-001-010',title:'Я, робот',genre:'фантастика',year:1950})
MERGE (:Book {id:'book11',isbn:'978-5-001-011',title:'Солярис',genre:'фантастика',year:1961})
MERGE (:Book {id:'book12',isbn:'978-5-001-012',title:'Идиот',genre:'классика',year:1869})
MERGE (:Book {id:'book13',isbn:'978-5-001-013',title:'Ревизор',genre:'драма',year:1836})
MERGE (:Book {id:'book14',isbn:'978-5-001-014',title:'Скотный двор',genre:'антиутопия',year:1945})
MERGE (:Book {id:'book15',isbn:'978-5-001-015',title:'Основание',genre:'фантастика',year:1951});

UNWIND [
['book1','auth1',1869],['book2','auth1',1877],['book3','auth2',1866],
['book12','auth2',1869],['book4','auth3',1833],['book5','auth4',1842],
['book13','auth4',1836],['book6','auth5',1904],['book7','auth6',1949],
['book14','auth6',1945],['book8','auth7',1977],['book9','auth8',1997],
['book10','auth9',1950],['book15','auth9',1951],['book11','auth10',1961]
] AS row
MATCH (b:Book {id:row[0]}),(a:Author {id:row[1]})
MERGE (b)-[:WRITTEN_BY {since:row[2],primary:true}]->(a);

UNWIND [
['copy1','AVAILABLE','2020-01-15'],['copy2','BORROWED','2019-06-01'],
['copy3','AVAILABLE','2021-03-10'],['copy4','RESERVED','2020-11-20'],
['copy5','AVAILABLE','2018-09-05'],['copy6','BORROWED','2022-02-14'],
['copy7','AVAILABLE','2021-07-22'],['copy8','BORROWED','2019-12-01'],
['copy9','AVAILABLE','2020-05-18'],['copy10','AVAILABLE','2022-01-03'],
['copy11','BORROWED','2021-08-11'],['copy12','AVAILABLE','2019-04-25'],
['copy13','RESERVED','2020-10-30'],['copy14','AVAILABLE','2022-06-07'],
['copy15','BORROWED','2018-03-12'],['copy16','AVAILABLE','2021-11-09'],
['copy17','AVAILABLE','2019-07-16'],['copy18','BORROWED','2020-02-28'],
['copy19','AVAILABLE','2022-04-01'],['copy20','AVAILABLE','2021-09-14']
] AS row
MERGE (:BookCopy {id:row[0],status:row[1],acquiredAt:date(row[2])});

UNWIND [
['copy1','book1'],['copy2','book1'],['copy3','book2'],['copy4','book3'],
['copy5','book4'],['copy6','book5'],['copy7','book6'],['copy8','book7'],
['copy9','book8'],['copy10','book9'],['copy11','book10'],['copy12','book11'],
['copy13','book12'],['copy14','book13'],['copy15','book14'],
['copy16','book15'],['copy17','book1'],['copy18','book3'],
['copy19','book7'],['copy20','book9']
] AS row
MATCH (c:BookCopy {id:row[0]}),(b:Book {id:row[1]})
MERGE (c)-[:INSTANCE_OF]->(b);

UNWIND [
['copy1','off1',1,'A1'],['copy2','off1',1,'A2'],['copy3','off1',2,'B1'],
['copy4','off2',1,'A3'],['copy5','off2',1,'C1'],['copy6','off2',2,'B2'],
['copy7','off3',1,'A4'],['copy8','off3',1,'D1'],['copy9','off3',2,'E1'],
['copy10','off4',1,'F1'],['copy11','off4',1,'A5'],['copy12','off5',3,'G1'],
['copy13','off5',2,'B3'],['copy14','off1',1,'A6'],['copy15','off2',2,'H1'],
['copy16','off5',1,'A7'],['copy17','off1',1,'A8'],['copy18','off3',2,'B4'],
['copy19','off2',1,'D2'],['copy20','off4',1,'F2']
] AS row
MATCH (c:BookCopy {id:row[0]}),(o:Office {id:row[1]})
MERGE (c)-[:LOCATED_AT {floor:row[2],shelf:row[3]}]->(o);

UNWIND [
['cl1','Иван Петров','ivan@mail.ru','Москва'],
['cl2','Мария Сидорова','maria@gmail.com','Москва'],
['cl3','Алексей Козлов','alex@yandex.ru','Санкт-Петербург'],
['cl4','Елена Новикова','elena@mail.ru','Казань'],
['cl5','Дмитрий Волков','dmitry@gmail.com','Москва'],
['cl6','Ольга Морозова','olga@yandex.ru','Новосибирск'],
['cl7','Сергей Соколов','sergey@mail.ru','Екатеринбург'],
['cl8','Анна Лебедева','anna@gmail.com','Москва'],
['cl9','Николай Кузнецов','nikolay@yandex.ru','Санкт-Петербург'],
['cl10','Татьяна Попова','tatyana@mail.ru','Казань'],
['cl11','Павел Фёдоров','pavel@gmail.com','Москва'],
['cl12','Наталья Михайлова','natalya@yandex.ru','Воронеж']
] AS row
MERGE (:Client {id:row[0],name:row[1],email:row[2],city:row[3]});

UNWIND [
['cl1','copy2','2024-01-10','2024-02-10',true],
['cl1','copy6','2024-03-01','2024-04-01',true],
['cl1','copy8','2024-05-15','2024-06-15',false],
['cl2','copy3','2024-02-01','2024-03-01',true],
['cl2','copy10','2024-04-10','2024-05-10',true],
['cl2','copy11','2024-06-01','2024-07-01',false],
['cl3','copy1','2023-11-01','2023-12-01',true],
['cl3','copy5','2024-01-20','2024-02-20',true],
['cl3','copy7','2024-05-01','2024-06-01',true],
['cl3','copy15','2024-07-01','2024-08-01',false],
['cl4','copy4','2024-03-15','2024-04-15',true],
['cl4','copy13','2024-06-10','2024-07-10',false],
['cl5','copy9','2024-02-20','2024-03-20',true],
['cl5','copy12','2024-04-01','2024-05-01',true],
['cl5','copy18','2024-08-01','2024-09-01',false],
['cl6','copy14','2023-12-10','2024-01-10',true],
['cl6','copy16','2024-05-20','2024-06-20',true],
['cl7','copy17','2024-01-05','2024-02-05',true],
['cl7','copy19','2024-06-15','2024-07-15',false],
['cl8','copy1','2024-03-10','2024-04-10',true],
['cl8','copy2','2024-07-01','2024-08-01',false],
['cl9','copy5','2024-02-15','2024-03-15',true],
['cl9','copy10','2024-05-01','2024-06-01',true],
['cl9','copy6','2024-09-01','2024-10-01',false],
['cl10','copy7','2024-04-20','2024-05-20',true],
['cl10','copy11','2024-08-10','2024-09-10',false],
['cl11','copy3','2024-01-01','2024-02-01',true],
['cl11','copy14','2024-06-01','2024-07-01',true],
['cl11','copy8','2024-09-15','2024-10-15',false],
['cl12','copy16','2024-03-01','2024-04-01',true],
['cl12','copy4','2024-07-20','2024-08-20',false],
['cl1','copy17','2023-10-01','2023-11-01',true],
['cl2','copy14','2023-09-15','2023-10-15',true],
['cl3','copy16','2024-04-01','2024-05-01',true],
['cl4','copy5','2024-01-01','2024-02-01',true],
['cl5','copy1','2024-07-10','2024-08-10',true],
['cl6','copy9','2024-03-01','2024-04-01',true],
['cl7','copy12','2024-04-15','2024-05-15',true],
['cl8','copy3','2024-05-20','2024-06-20',true],
['cl9','copy14','2024-06-10','2024-07-10',true],
['cl10','copy16','2024-02-01','2024-03-01',true],
['cl11','copy17','2024-08-01','2024-09-01',true],
['cl12','copy7','2024-05-01','2024-06-01',true]
] AS row
MATCH (cl:Client {id:row[0]}),(c:BookCopy {id:row[1]})
MERGE (cl)-[:BORROWED {since:date(row[2]),until:date(row[3]),returned:row[4]}]->(c);

UNWIND [
['cl1','copy4','2024-09-01','2024-09-15'],
['cl2','copy13','2024-08-05','2024-08-20'],
['cl4','copy9','2024-10-01','2024-10-15'],
['cl5','copy1','2024-09-10','2024-09-25'],
['cl8','copy12','2024-08-15','2024-08-30']
] AS row
MATCH (cl:Client {id:row[0]}),(c:BookCopy {id:row[1]})
MERGE (cl)-[:RESERVED {from:date(row[2]),to:date(row[3])}]->(c);

UNWIND [
['cl1','book1',5,'2024-01-15'],['cl1','book7',4,'2024-03-20'],
['cl2','book9',5,'2024-02-10'],['cl2','book1',4,'2024-04-05'],
['cl3','book11',5,'2024-01-25'],['cl3','book7',4,'2024-05-12'],
['cl4','book3',5,'2024-03-01'],['cl5','book10',4,'2024-04-18'],
['cl5','book15',5,'2024-06-01'],['cl6','book9',5,'2024-02-20'],
['cl7','book8',4,'2024-05-05'],['cl8','book1',5,'2024-01-10'],
['cl8','book9',4,'2024-06-15'],['cl9','book7',5,'2024-03-15'],
['cl10','book6',4,'2024-07-01'],['cl11','book11',5,'2024-04-01'],
['cl12','book15',4,'2024-08-01']
] AS row
MATCH (cl:Client {id:row[0]}),(b:Book {id:row[1]})
MERGE (cl)-[:RECOMMENDS {rating:row[2],since:date(row[3])}]->(b);

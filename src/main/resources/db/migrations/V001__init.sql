create table clients
(
    id         serial primary key,
    name       varchar(255)                not null,
    email      varchar(255)                not null unique,
    city       varchar(255)                not null,
    password   text                        not null,
    created_at timestamp without time zone not null default now()
);

create table users_staff
(
    id         serial primary key,
    name       varchar(255)                not null,
    email      varchar(255)                not null unique,
    city       varchar(255)                not null,
    password   text                        not null,
    created_at timestamp without time zone not null default now()
);

create table offices
(
    id           serial primary key,
    name         varchar(255) not null,
    address      varchar(255) not null,
    working_time varchar(255) not null
);

create table book_link
(
    id       serial primary key,
    mongo_id varchar(255) not null unique
);

create table book_copies
(
    id           serial primary key,
    book_link_id int                         not null,
    office_id    int                         not null,
    status       varchar(255)                not null,
    created_at   timestamp without time zone not null default now(),
    constraint fk_book_link foreign key (book_link_id)
        references book_link (id)
        on delete cascade,
    constraint fk_office foreign key (office_id)
        references offices (id)
        on delete restrict
);

create table reservations
(
    id           serial primary key,
    book_copy_id int                         not null,
    client_id    int                         not null,
    starts_at    timestamp without time zone not null,
    ends_at      timestamp without time zone not null,
    constraint fk_client foreign key (client_id)
        references clients (id)
        on delete restrict,
    constraint fk_book_copy foreign key (book_copy_id)
        references book_copies (id)
        on delete cascade
);

create table loans
(
    id           serial primary key,
    book_copy_id int                         not null,
    client_id    int                         not null,
    status       varchar(255)                not null,
    starts_at    timestamp without time zone not null,
    ends_at      timestamp without time zone not null,
    constraint fk_clients foreign key (client_id)
        references clients (id)
        on delete restrict,
    constraint fk_book_copy foreign key (book_copy_id)
        references book_copies (id)
        on delete cascade
);

create table returns
(
    id          serial primary key,
    loan_id     int                         not null,
    returned_at timestamp without time zone not null default now(),
    constraint fk_loan foreign key (loan_id)
        references loans (id)
        on delete cascade
);

create table fines
(
    id         serial primary key,
    loan_id    int                         not null,
    amount     numeric(5, 2)               not null,
    status     varchar(255)                not null,
    created_at timestamp without time zone not null default now(),
    constraint fk_loan foreign key (loan_id)
        references loans (id)
        on delete cascade
);
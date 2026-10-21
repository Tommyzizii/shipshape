create table app_user (
    id              bigint generated always as identity primary key,
    email           text            not null unique,
    password_hash   text            not null,
    created_at      timestamptz     not null default now()
);
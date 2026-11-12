create table project (
    id              bigint          generated always as identity primary key,
    owner_id        bigint          not null,
    name            text            not null,
    created_at      timestamptz     not null default now(),
    constraint project_owner_id_fkey foreign key (owner_id) references app_user (id) on delete cascade,
    constraint project_name_length_check check (char_length(name) between 1 and 100)
);

-- One user cannot have two projects with the same name (ignoring case).
-- owner_id comes first, so this index also serves "list my projects" queries.
create unique index project_owner_id_name_lower_key
    on project (owner_id, lower(name));

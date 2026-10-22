alter table app_user
    drop constraint app_user_email_key;

create unique index app_user_email_lower_key
    on app_user (lower(email));
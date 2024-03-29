create table if not exists transactions
(
    transaction_id bigint default nextval('transactions_id_seq'::regclass) not null
        constraint transactions_pk
            primary key,
    sender_id      bigint                                                  not null,
    recipient_id   bigint                                                  not null,
    sum            bigint default 0                                        not null,
    time           timestamp                                               not null,
    comment        varchar,
    chat_id        bigint                                                  not null
);

create table if not exists users
(
    user_id       bigint default nextval('users_id_seq'::regclass) not null
        constraint users_pk
            primary key,
    telegram_name varchar                                          not null
        unique
);

create table if not exists debts
(
    sum          bigint default 0 not null,
    sender_id    bigint           not null,
    recipient_id bigint           not null,
    chat_id      bigint           not null,
    constraint debts_pk
        primary key (sender_id, recipient_id, chat_id),
    constraint unique_sender_recipient_chat
        unique (sender_id, recipient_id, chat_id)
);

create unique index idx_unique_sender_recipient
    on debts (LEAST(sender_id, recipient_id), GREATEST(sender_id, recipient_id), chat_id);

create table if not exists active_session_tokens
(
    user_id         bigserial,
    identifier_hash varchar   not null,
    expiration_time timestamp not null
);



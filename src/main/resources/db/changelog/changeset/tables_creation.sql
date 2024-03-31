create table if not exists transactions
(
    transaction_id BIGSERIAL PRIMARY KEY                                   not null,
    sender_id      bigint                                                  not null,
    recipient_id   bigint                                                  not null,
    sum            bigint default 0                                        not null,
    time           timestamp                                               not null,
    comment        varchar,
    chat_id        bigint                                                  not null
);

create table if not exists users
(
    user_id       bigint primary key                               not null,
    telegram_name varchar                                          not null
        unique
);

CREATE TABLE IF NOT EXISTS debts (
     id BIGSERIAL PRIMARY KEY,
     sum bigint DEFAULT 0 NOT NULL,
     sender_id bigint NOT NULL,
     recipient_id bigint NOT NULL,
     chat_id bigint NOT NULL,
     CONSTRAINT unique_sender_recipient_chat UNIQUE (sender_id, recipient_id, chat_id)
);

CREATE UNIQUE INDEX idx_unique_sender_recipient
    ON debts (LEAST(sender_id, recipient_id), GREATEST(sender_id, recipient_id), chat_id);
create table if not exists active_session_tokens
(
    user_id         bigserial not null,
    identifier_hash varchar   not null,
    expiration_time timestamp not null
);
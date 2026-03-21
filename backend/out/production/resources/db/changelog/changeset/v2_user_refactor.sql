-- Drop all old tables (no data migration needed)
DROP TABLE IF EXISTS debts CASCADE;
DROP TABLE IF EXISTS transactions CASCADE;
DROP TABLE IF EXISTS active_session_tokens CASCADE;
DROP TABLE IF EXISTS users CASCADE;

-- New users table: supports both Telegram and web auth
CREATE TABLE users (
    id            BIGSERIAL PRIMARY KEY,
    telegram_id   BIGINT UNIQUE,
    telegram_name VARCHAR,
    username      VARCHAR UNIQUE,
    password_hash VARCHAR
);

-- Transactions reference the new internal user id
CREATE TABLE transactions (
    transaction_id BIGSERIAL PRIMARY KEY NOT NULL,
    sender_id      BIGINT NOT NULL REFERENCES users(id),
    recipient_id   BIGINT NOT NULL REFERENCES users(id),
    sum            BIGINT NOT NULL DEFAULT 0,
    time           TIMESTAMP NOT NULL,
    comment        VARCHAR,
    chat_id        BIGINT NOT NULL,
    CONSTRAINT check_tx_sender_not_recipient CHECK (sender_id != recipient_id)
);

-- Session tokens for Telegram-based login (user_id = users.id)
CREATE TABLE active_session_tokens (
    user_id         BIGINT NOT NULL,
    identifier_hash VARCHAR NOT NULL,
    expiration_time TIMESTAMP NOT NULL
);

-- Link tokens for connecting Telegram to a web account
CREATE TABLE link_tokens (
    token      VARCHAR PRIMARY KEY,
    user_id    BIGINT NOT NULL REFERENCES users(id),
    expires_at TIMESTAMP NOT NULL
);

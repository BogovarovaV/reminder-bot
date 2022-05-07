-- liquibase formatted sql

-- changeset vbogovarova:1
CREATE TABLE reminders
(
    id            SERIAL NOT NULL PRIMARY KEY,
    chat_id       BIGINT NOT NULL,
    reminder_text TEXT NOT NULL,
    date_time     TIMESTAMP NOT NULL
);
package ru.m_polukhin.debtsapp.models;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.sql.Timestamp;

@Table("active_session_tokens")
public record ActiveSessionToken (
    @Id
    @Column("user_id")
    Long userId,

    @Column("identifier_hash")
    String hash,

    @Column("expiration_time")
    Timestamp expirationDate
) {}
package ru.m_polukhin.debtsapp.models;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

@Table("users")
public record UserData (
        @Id
        @Column("user_id")
        Long id,

        @Column("telegram_name")
        String telegramName
) {}

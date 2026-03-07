package ru.m_polukhin.debtsapp.models;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

@Data
@Table("users")
@AllArgsConstructor
@NoArgsConstructor
public class UserData {
    @Id
    @Column("id")
    private Long id;

    @Column("telegram_name")
    private String telegramName;

    @Column("telegram_id")
    private Long telegramId;

    @Column("username")
    private String username;

    @Column("password_hash")
    private String passwordHash;
}

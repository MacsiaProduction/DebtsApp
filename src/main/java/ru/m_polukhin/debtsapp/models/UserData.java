package ru.m_polukhin.debtsapp.models;

import javax.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@NoArgsConstructor
@Table(name = "users")
public final class UserData {
    public UserData(Long id, String telegramName) {
        this.id = id;
        this.telegramName = telegramName;
    }

    @Id
    @Column(name = "user_id", unique = true, nullable = false)
    private Long id;

    @Column(name = "telegram_name", unique = true, nullable = false)
    private String telegramName;
}

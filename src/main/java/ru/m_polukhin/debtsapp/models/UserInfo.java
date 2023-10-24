package ru.m_polukhin.debtsapp.models;

import javax.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@NoArgsConstructor
@Table(name = "users")
public final class UserInfo {
    public UserInfo(Long id, String telegramName) {
        this.id = id;
        this.telegramName = telegramName;
    }

    @Id
    @Column(name = "user_id")
    private Long id;

    @Column(name = "telegram_name")
    private String telegramName;
}

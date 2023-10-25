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

    public UserData(Long id, String telegramName, String passwordHash) {
        this.id = id;
        this.telegramName = telegramName;
        this.passwordHash = passwordHash;
    }

    @Id
    @Column(name = "user_id", unique = true, nullable = false)
    private Long id;

    @Column(name = "telegram_name", unique = true, nullable = false)
    private String telegramName;

    @Column(name = "password_hash")
    private String passwordHash;

    public String getRole() {
        return "User";
    }
}

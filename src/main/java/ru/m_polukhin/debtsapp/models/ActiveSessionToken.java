package ru.m_polukhin.debtsapp.models;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import javax.persistence.*;
import java.sql.Timestamp;

@Getter
@Entity
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "active_session_tokens")
public final class ActiveSessionToken {
    @Id
    @Column(name = "user_id", unique = true, nullable = false)
    private Long userId;

    @Column(name = "identifier_hash", unique = true, nullable = false)
    private String hash;

    @Column(name = "expiration_time", nullable = false)
    private Timestamp expirationDate;
}

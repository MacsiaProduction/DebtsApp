package ru.m_polukhin.debtsapp.models;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;

import javax.persistence.*;
import java.io.Serializable;

@Entity
@NoArgsConstructor
@Table(name = "debts")
public final class Debt {
    @EmbeddedId
    private DebtId id;

    @Getter
    @Column(name = "sum")
    private Long sum;

    public Long getRecipientId() {
        return id.getRecipientId();
    }

    public Long getSenderId() {
        return id.getSenderId();
    }

    public Long getChatId() {
        return id.getChatId();
    }

    @Embeddable
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static final class DebtId implements Serializable {
        @Column(name = "sender_id")
        private Long senderId;

        @Column(name = "recipient_id")
        private Long recipientId;

        @Column(name = "chat_id")
        private Long chatId;
    }
}

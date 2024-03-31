package ru.m_polukhin.debtsapp.models;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;


@Data
@NoArgsConstructor
@AllArgsConstructor
@Table("debts")
public final class Debt {
    @Id
    private DebtId id;

    @Column("sum")
    private Long sum;

    public Long getSenderId() {
        return id.getSenderId();
    }

    public Long getRecipientId() {
        return id.getRecipientId();
    }

    public Long getChatId() {
        return id.getChatId();
    }
}

@Data
@NoArgsConstructor
@AllArgsConstructor
class DebtId {
    @Column("sender_id")
    private Long senderId;

    @Column("recipient_id")
    private Long recipientId;

    @Column("chat_id")
    private Long chatId;
}
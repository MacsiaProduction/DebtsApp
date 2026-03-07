package ru.m_polukhin.debtsapp.models;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;
import ru.m_polukhin.debtsapp.exceptions.ParseException;
import ru.m_polukhin.debtsapp.exceptions.UserNotFoundException;

import java.sql.Timestamp;

@Data
@Table(name="transactions")
public final class Transaction {
    @Id
    @Column("transaction_id")
    private Long id;

    @Column("sum")
    private final Long sum;

    @Column("sender_id")
    private final Long senderId;

    @Column("recipient_id")
    private final Long recipientId;

    @Column("chat_id")
    private final Long chatId;

    @Column("comment")
    private final String comment;

    @Column("time")
    private Timestamp timestamp;

    public Transaction(Long sum, Long senderId, Long recipientId, Long chatId, String comment) throws ParseException, UserNotFoundException {
        this.sum = sum;
        this.senderId = senderId;
        this.recipientId = recipientId;
        this.chatId = chatId;
        this.comment = comment;
        this.timestamp = new Timestamp(System.currentTimeMillis());
        if (recipientId.equals(senderId)) throw new UserNotFoundException("Me");
        if (sum < 0) throw new ParseException("Value of transaction should be positive");
    }
}
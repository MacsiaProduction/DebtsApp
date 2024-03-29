package ru.m_polukhin.debtsapp.models;

import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import ru.m_polukhin.debtsapp.exceptions.ParseException;
import ru.m_polukhin.debtsapp.exceptions.UserNotFoundException;

import javax.persistence.*;
import java.sql.Timestamp;

@Entity
@Getter
@NoArgsConstructor
@Table(name="transactions")
public final class Transaction {
    @Id
    @Column(name = "transaction_id")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "sum")
    private Long sum;

    @Column(name = "sender_id")
    private Long senderId;

    @Column(name = "recipient_id")
    private Long recipientId;

    @Column(name = "chat_id")
    private Long chatId;

    @Column(name = "comment")
    private String comment;

    @CreationTimestamp
    @Column(name = "time")
    private Timestamp timestamp;

    public Transaction(Long sum, Long senderId, Long recipientId, Long chatId, String comment) throws ParseException, UserNotFoundException {
        this.sum = sum;
        this.senderId = senderId;
        this.recipientId = recipientId;
        this.chatId = chatId;
        this.comment = comment;
        if (recipientId.equals(senderId)) throw new UserNotFoundException("Me");
        if (sum < 0) throw new ParseException("Value of transaction should be positive");
    }
}

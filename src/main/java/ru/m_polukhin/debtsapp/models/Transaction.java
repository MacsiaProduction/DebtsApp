package ru.m_polukhin.debtsapp.models;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

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

    @CreationTimestamp
    @Column(name = "time")
    private Timestamp timestamp;

    public Transaction(Long sum, Long senderId, Long recipientId) {
        this.sum = sum;
        this.senderId = senderId;
        this.recipientId = recipientId;
    }
}

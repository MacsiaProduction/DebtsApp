package ru.m_polukhin.debtsapp.models;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

@Data
@Table("debts")
@AllArgsConstructor
@NoArgsConstructor
public class Debt {
    @Id
    @Column("id")
    private Long id; // Surrogate key

    @Column("sender_id")
    private Long senderId;

    @Column("recipient_id")
    private Long recipientId;

    @Column("chat_id")
    private Long chatId;

    @Column("sum")
    private Long sum;
}
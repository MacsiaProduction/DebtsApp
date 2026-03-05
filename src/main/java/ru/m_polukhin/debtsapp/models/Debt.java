package ru.m_polukhin.debtsapp.models;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class Debt {
    private Long senderId;
    private Long recipientId;
    private Long chatId;
    private Long amount;
}

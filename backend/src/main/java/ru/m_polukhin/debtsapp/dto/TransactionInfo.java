package ru.m_polukhin.debtsapp.dto;

import jakarta.annotation.Nonnull;

// todo отрефакторить, использовать DTO
public record TransactionInfo(String sender, String recipient, Long sum, Long chatId, String comment) {
    @Nonnull
    @Override
    public String toString() {
        return sender + " -> " + recipient + "{"+sum+"} " + comment;
    }

}

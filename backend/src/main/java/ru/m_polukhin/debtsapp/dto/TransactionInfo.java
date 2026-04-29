package ru.m_polukhin.debtsapp.dto;

import jakarta.annotation.Nonnull;

public record TransactionInfo(Long id, String sender, String recipient, Long sum, Long chatId, String comment) {
    @Nonnull
    @Override
    public String toString() {
        return sender + " -> " + recipient + "{" + sum + "} " + comment;
    }
}

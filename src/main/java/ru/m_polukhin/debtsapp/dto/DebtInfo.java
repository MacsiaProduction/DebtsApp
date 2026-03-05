package ru.m_polukhin.debtsapp.dto;

import jakarta.annotation.Nonnull;

// todo отрефакторить, использовать DTO
public record DebtInfo(String from, String to, Long sum, Long chatId) {
    @Nonnull
    @Override
    public String toString() {
        if (sum>0) {
            return from + " <₽-" + to + "{"+sum+"}";
        } else {
            return to + " <₽-" + from + "{"+(-sum)+"}";
        }
    }
}

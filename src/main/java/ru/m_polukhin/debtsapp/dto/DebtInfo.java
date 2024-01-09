package ru.m_polukhin.debtsapp.dto;

public record DebtInfo(String from, String to, Long sum, Long chatId) {
    @Override
    public String toString() {
        if (sum>0) {
            return from + " <₽-" + to + "{"+sum+"}";
        } else {
            return to + " <₽-" + from + "{"+(-sum)+"}";
        }
    }
}

package ru.m_polukhin.debtsapp.dto;

//todo refactor to use dto
public record TransactionInfo(String sender, String recipient, Long sum, Long chatId, String comment) {
    @Override
    public String toString() {
        return sender + " -> " + recipient + "{"+sum+"} " + comment;
    }

}

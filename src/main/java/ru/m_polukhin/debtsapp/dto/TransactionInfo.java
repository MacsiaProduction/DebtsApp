package ru.m_polukhin.debtsapp.dto;

public record TransactionInfo(String sender, String recipient, Long sum) {
    @Override
    public String toString() {
        return sender + " -> " + recipient + "{"+sum+"}";
    }

}

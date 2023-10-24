package ru.m_polukhin.debtsapp.models;

public record DebtInfo(String from, String to, Long sum) {
    @Override
    public String toString() {
        if (sum>0) {
            return from + " <$-" + to + "{"+sum+"}";
        } else {
            return to + " <$-" + from + "{"+(-sum)+"}";
        }
    }
}

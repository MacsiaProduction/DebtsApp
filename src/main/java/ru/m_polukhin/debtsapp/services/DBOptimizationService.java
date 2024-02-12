package ru.m_polukhin.debtsapp.services;

import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class DBOptimizationService {
    private final DebtsDAO dao;

    @Scheduled(fixedRate = 1, timeUnit = TimeUnit.HOURS)
    public void deleteZeroSumDebts() {
        dao.deleteZeroSumDebts();
    }

    public void deleteDeletedChats(Long chatId) {
        dao.deleteChatHistory(chatId);
    }
}

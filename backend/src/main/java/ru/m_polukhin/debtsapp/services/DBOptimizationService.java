package ru.m_polukhin.debtsapp.services;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class DBOptimizationService {
    private final DebtsDAO dao;

    public void deleteDeletedChats(Long chatId) {
        dao.deleteChatHistory(chatId);
    }
}

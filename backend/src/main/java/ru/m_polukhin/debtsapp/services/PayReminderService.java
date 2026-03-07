package ru.m_polukhin.debtsapp.services;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import ru.m_polukhin.debtsapp.exceptions.UserNotFoundException;

import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Slf4j
@RequiredArgsConstructor
public class PayReminderService {
    private final TelegramService telegramService;
    private final DebtsDAO dao;

    @Scheduled(fixedRate = 7, timeUnit = TimeUnit.DAYS)
    public void sendRegularMessage() {
        for (var chatId : dao.getAllChats()) {
            try {
                var debts = dao.getAllDebtsInChat(chatId, PageRequest.of(0, 20));
                var text = debts.getContent().stream()
                        .map(Object::toString)
                        .collect(Collectors.joining("\n"));
                telegramService.sendMessage(chatId, "Hi! It's debt collection time again!\n" + text);
            } catch (UserNotFoundException e) {
                log.warn("Could not load debts for chat {}: {}", chatId, e.getMessage());
            }
        }
    }
}

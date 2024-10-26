package ru.m_polukhin.debtsapp.services;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import ru.m_polukhin.debtsapp.controllers.MessageUtil;

import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@RequiredArgsConstructor
public class PayReminderService {
    private final TelegramService telegramService;
    private final DebtsDAO dao;

    @Scheduled(fixedRate = 7, timeUnit = TimeUnit.DAYS, initialDelay = 7)
    public void sendRegularMessage() {
        var chats = dao.getAllChats();
        for (var chatId : chats) {
            var debts = dao.getAllDebtsInChat(chatId, PageRequest.of(0, 30));
            List<String> res2 = debts.stream()
                    .map(MessageUtil::oweMessageWithDog)
                    .collect(Collectors.toList());
            var text = String.join("\n", res2);
            telegramService.sendMessage(chatId, "Hi! It's debt collection time again!\n" + text);
        }
    }
}

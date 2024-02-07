package ru.m_polukhin.debtsapp.services;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import ru.m_polukhin.debtsapp.dto.DebtInfo;

import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PayReminderService {
    private final TelegramService telegramService;
    private final DebtsDAO dao;

    @Scheduled(fixedRate = 7, timeUnit = TimeUnit.DAYS)
    public void sendRegularMessage() {
        var chats = dao.getAllChats();
        for (var chatId : chats) {
            var debts = dao.getAllDebtsInChat(chatId, PageRequest.of(0, 20));
            List<String> res2 = debts.stream()
                    .map(DebtInfo::toString)
                    .collect(Collectors.toList());
            var text = String.join("\n", res2);
            telegramService.sendMessage(chatId, "Hi! It's debt collection time again!\n" + text);
        }
    }
}

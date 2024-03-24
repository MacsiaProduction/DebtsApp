package ru.m_polukhin.debtsapp.services;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.bots.DefaultAbsSender;
import org.telegram.telegrambots.bots.DefaultBotOptions;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import ru.m_polukhin.debtsapp.configs.BotConfig;

@Service
public class TelegramService extends DefaultAbsSender {
    private final DBOptimizationService dbOptimizationService;
    public TelegramService(BotConfig config, @Autowired DBOptimizationService dbOptimizationService) {
        super(new DefaultBotOptions(), config.getToken());
        this.dbOptimizationService = dbOptimizationService;
    }

    public void sendMessage(Long chatId, String text) {
        sendMessage(chatId, null, text);
    }

    public void sendMessage(Long chatId, Integer threadId, String text) {
        SendMessage sendMessage;
        if (text.isEmpty()) {
            sendMessage = new SendMessage(
                    chatId.toString(),
                    threadId,
                    "Nothing to show",
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null);
        } else {
            sendMessage = new SendMessage(
                    chatId.toString(),
                    threadId,
                    text,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null);
        }
        //todo refactor
        try {
            execute(sendMessage);
        } catch (TelegramApiException e) {
            if (e.getMessage().contains("[403]") || e.getMessage().contains("[404]")) {
                dbOptimizationService.deleteDeletedChats(chatId);
            }
        }
    }
}

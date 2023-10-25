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
    @Autowired
    protected TelegramService(BotConfig config) {
        super(new DefaultBotOptions(), config.getToken());
    }

    public void sendMessage(Long chatId, String text) {
        var chatIdStr = String.valueOf(chatId);
        SendMessage sendMessage;
        if (text.isEmpty()) {
            sendMessage = new SendMessage(chatIdStr, "Nothing to show");
        } else {
            sendMessage = new SendMessage(chatIdStr, text);
        }
        try {
            execute(sendMessage);
        } catch (TelegramApiException e) {
            throw new RuntimeException(e);
        }
    }
}

package ru.m_polukhin.debtsapp.configs;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;
import ru.m_polukhin.debtsapp.controllers.TelegramController;

@Configuration
public class BotConfiguration {
    @Bean
    public TelegramBotsApi telegramBotsApi(TelegramController bot) throws TelegramApiException {
        var api = new TelegramBotsApi(DefaultBotSession.class);
        api.registerBot(bot);
        return api;
    }

}
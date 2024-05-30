package ru.m_polukhin.debtsapp.configs;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.EnableScheduling;
import ru.m_polukhin.debtsapp.services.DebtsDAO;
import ru.m_polukhin.debtsapp.services.PayReminderService;
import ru.m_polukhin.debtsapp.services.TelegramService;

@Configuration
@EnableScheduling
@Profile("main")
public class SchedulingConfig {
    @Bean
    public PayReminderService payReminderService(TelegramService telegramService, DebtsDAO dao) {
        return new PayReminderService(telegramService, dao);
    }
}
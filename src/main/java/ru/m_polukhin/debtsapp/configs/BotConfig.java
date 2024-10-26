package ru.m_polukhin.debtsapp.configs;

import io.github.cdimascio.dotenv.Dotenv;
import lombok.Data;
import org.springframework.context.annotation.Configuration;

@Configuration
@Data
public class BotConfig {
    public BotConfig() {
        Dotenv dotenv = Dotenv.load();
        this.token = dotenv.get("BOT_TOKEN");
        this.botName = dotenv.get("BOT_NAME");
    }
    String botName;
    String token;
}

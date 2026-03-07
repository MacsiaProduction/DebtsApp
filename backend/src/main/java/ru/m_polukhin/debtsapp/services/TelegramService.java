package ru.m_polukhin.debtsapp.services;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.bots.DefaultAbsSender;
import org.telegram.telegrambots.bots.DefaultBotOptions;
import org.telegram.telegrambots.meta.api.methods.reactions.SetMessageReaction;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.reactions.ReactionType;
import org.telegram.telegrambots.meta.api.objects.reactions.ReactionTypeEmoji;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import ru.m_polukhin.debtsapp.configs.BotConfig;

import java.util.List;

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
        if (text.isEmpty()) {
            text = "Nothing to show";
        }
        var sendMessage = new SendMessage(
                chatId.toString(),
                threadId,
                text,
                null,
                true,
                true,
                null,
                null,
                null,
                null,
                null,
                null,
                null);
        try {
           execute(sendMessage);
        } catch (TelegramApiException e) {
            if (e.getMessage().contains("[403]") || e.getMessage().contains("[404]")) {
                dbOptimizationService.deleteDeletedChats(chatId);
            }
        }
    }

    public void markAsRead(Long chatId, Integer messageId) {
        ReactionTypeEmoji reactionTypeEmoji = new ReactionTypeEmoji(ReactionType.EMOJI_TYPE, "\uD83D\uDC4D");
        var response = new SetMessageReaction(chatId.toString(), messageId, List.of(reactionTypeEmoji), false);
        try {
            execute(response);
        } catch (TelegramApiException e) {
            if (e.getMessage().contains("[403]") || e.getMessage().contains("[404]")) {
                dbOptimizationService.deleteDeletedChats(chatId);
            }
        }
    }
}

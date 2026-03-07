package ru.m_polukhin.debtsapp.controllers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Controller;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.User;
import ru.m_polukhin.debtsapp.configs.BotConfig;
import ru.m_polukhin.debtsapp.exceptions.ParseException;
import ru.m_polukhin.debtsapp.exceptions.UserNotFoundException;
import ru.m_polukhin.debtsapp.services.DebtsDAO;
import ru.m_polukhin.debtsapp.services.SecurityService;
import ru.m_polukhin.debtsapp.services.TelegramService;
import ru.m_polukhin.debtsapp.utils.Calculator;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Controller
public class TelegramController extends TelegramLongPollingBot {
    private final DebtsDAO dao;
    private final TelegramService telegramService;
    private final SecurityService securityService;
    private final String botName;

    private static final String START = "/start";
    private static final String ADD = "/add";
    private static final String GET = "/get";
    private static final String HISTORY = "/history";
    private static final String HELP = "/help";
    private static final String DEBTS = "/debts";
    private static final String LINK = "/link";

    @Autowired
    public TelegramController(DebtsDAO dao, TelegramService telegramService, SecurityService securityService, BotConfig config) {
        super(config.getToken());
        this.dao = dao;
        this.telegramService = telegramService;
        this.securityService = securityService;
        this.botName = config.getBotName();
    }

    @Override
    public void onUpdateReceived(Update update) {
        if (!update.hasMessage() || !update.getMessage().hasText()) return;

        var message = update.getMessage().getText();
        var chatId = update.getMessage().getChatId();
        var threadId = update.getMessage().getMessageThreadId();
        var messageId = update.getMessage().getMessageId();
        var user = update.getMessage().getFrom();
        var messageSplit = message.split(" ");
        var command = messageSplit[0];

        // Убрать упоминание бота (например, /start@BotName → /start)
        int atIdx = command.indexOf('@');
        if (atIdx > 0) command = command.substring(0, atIdx);

        switch (command) {
            case START -> {
                if (messageSplit.length == 1) startCommand(chatId, threadId, user);
                else activateSession(chatId, threadId, user, messageSplit[1]);
            }
            case LINK -> linkCommand(chatId, threadId, user, messageSplit);
            case ADD -> addCommand(chatId, threadId, messageId, user, messageSplit);
            case HELP -> helpCommand(chatId, threadId);
            case GET -> getCommand(chatId, threadId, user, messageSplit);
            case HISTORY -> historyCommand(chatId, threadId, user, messageSplit);
            case DEBTS -> debtsCommand(chatId, threadId, user, messageSplit);
            default -> unknownCommand(chatId, threadId);
        }
    }

    private void startCommand(Long chatId, Integer threadId, User user) {
        dao.addTelegramUser(user.getId(), user.getUserName());
        telegramService.sendMessage(chatId, threadId,
                String.format("Well Cum to our club, %s!\n", user.getUserName()));
        helpCommand(chatId, threadId);
    }

    private void linkCommand(Long chatId, Integer threadId, User user, String[] messageSplit) {
        if (messageSplit.length != 2) {
            telegramService.sendMessage(chatId, threadId, "Usage: /link {token}");
            return;
        }
        boolean linked = securityService.linkTelegramAccount(user.getId(), user.getUserName(), messageSplit[1]);
        telegramService.sendMessage(chatId, threadId,
                linked ? "Telegram account linked successfully!" : "Invalid or expired link token.");
    }

    private void addCommand(Long chatId, Integer threadId, Integer messageId, User user, String[] messageSplit) {
        try {
            if (messageSplit.length < 3) throw new ParseException("Wrong argument count");
            Long userId = dao.getIdByTelegramId(user.getId());
            String recipient = messageSplit[1].startsWith("@") ? messageSplit[1].substring(1) : messageSplit[1];
            String comment = messageSplit.length > 3
                    ? String.join(" ", Arrays.copyOfRange(messageSplit, 3, messageSplit.length)) : "";
            Long sum = Calculator.evaluateExpression(messageSplit[2]);
            dao.addTransaction(chatId, userId, recipient, sum, comment);
            telegramService.markAsRead(chatId, messageId);
        } catch (ParseException e) {
            telegramService.sendMessage(chatId, threadId, "Wrong format: " + e.getMessage());
        } catch (UserNotFoundException e) {
            telegramService.sendMessage(chatId, threadId, "User " + e.getMessage() + " not found");
        }
    }

    private void getCommand(Long chatId, Integer threadId, User user, String[] messageSplit) {
        try {
            if (messageSplit.length != 2) throw new ParseException("Wrong argument count");
            String recipient = messageSplit[1].startsWith("@") ? messageSplit[1].substring(1) : messageSplit[1];
            telegramService.sendMessage(chatId, threadId,
                    dao.getDebt(chatId, user.getUserName(), recipient).toString());
        } catch (ParseException e) {
            telegramService.sendMessage(chatId, threadId, "Wrong format: " + e.getMessage());
        } catch (UserNotFoundException e) {
            telegramService.sendMessage(chatId, threadId, "User " + e.getMessage() + " not registered");
        }
    }

    private void historyCommand(Long chatId, Integer threadId, User user, String[] messageSplit) {
        try {
            int pageNum = messageSplit.length == 2 ? Integer.parseInt(messageSplit[1]) : 0;
            Long userId = dao.getIdByTelegramId(user.getId());
            var res = dao.findAllTransactionsRelated(chatId, userId, PageRequest.of(pageNum, 20));
            List<String> lines = new ArrayList<>();
            res.forEach(t -> lines.add(t.toString()));
            telegramService.sendMessage(chatId, threadId, String.join("\n", lines));
        } catch (UserNotFoundException e) {
            telegramService.sendMessage(chatId, threadId, "You aren't registered, try /start");
        } catch (NumberFormatException e) {
            telegramService.sendMessage(chatId, threadId, "Wrong format: " + e.getMessage());
        }
    }

    private void debtsCommand(Long chatId, Integer threadId, User user, String[] messageSplit) {
        try {
            int pageNum = messageSplit.length == 2 ? Integer.parseInt(messageSplit[1]) : 0;
            Long userId = dao.getIdByTelegramId(user.getId());
            var res = dao.findAllDebtsRelated(chatId, userId, PageRequest.of(pageNum, 20));
            telegramService.sendMessage(chatId, threadId,
                    String.join("\n", res.map(Object::toString).toList()));
        } catch (UserNotFoundException e) {
            telegramService.sendMessage(chatId, threadId, "User " + e.getMessage() + " not registered");
        } catch (NumberFormatException e) {
            telegramService.sendMessage(chatId, threadId, "Wrong format: " + e.getMessage());
        }
    }

    private void helpCommand(Long chatId, Integer threadId) {
        telegramService.sendMessage(chatId, threadId, """
                /add @Username {sum} {comment} - add transaction Me→Someone
                /get @Username - check debt between you and them
                /history {page} - related transactions (page 0 by default)
                /debts {page} - related debts (page 0 by default)
                /link {token} - link this Telegram account to a web account
                """);
    }

    private void unknownCommand(Long chatId, Integer threadId) {
        telegramService.sendMessage(chatId, threadId, "Unknown command. Try /help");
    }

    private void activateSession(Long chatId, Integer threadId, User user, String token) {
        try {
            Long userId = dao.getIdByTelegramId(user.getId());
            securityService.activateSessionToken(userId, token);
            telegramService.sendMessage(chatId, threadId, "Session authenticated! You have 1 minute ;)");
        } catch (UserNotFoundException e) {
            telegramService.sendMessage(chatId, threadId, "You aren't registered, try /start");
        }
    }

    @Override
    public String getBotUsername() {
        return botName;
    }
}

package ru.m_polukhin.debtsapp.controllers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Controller;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.User;
import ru.m_polukhin.debtsapp.services.DebtsDAO;
import ru.m_polukhin.debtsapp.services.SecurityService;
import ru.m_polukhin.debtsapp.services.TelegramService;
import ru.m_polukhin.debtsapp.configs.BotConfig;
import ru.m_polukhin.debtsapp.exceptions.ParseException;
import ru.m_polukhin.debtsapp.exceptions.UserNotFoundException;

import java.util.ArrayList;
import java.util.List;


@Controller
public class TelegramController extends TelegramLongPollingBot {
    //todo adding to group
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
        var user = update.getMessage().getFrom();
        var username = user.getUserName();
        var messageSplit = message.split(" ");
        var command = messageSplit[0];

        switch (command) {
            case START -> {
                var len = messageSplit.length;
                if (len == 1) {
                    startCommand(chatId, user);
                } else {
                    activateSession(chatId, user, messageSplit[1]);
                }
            }
            case ADD -> addCommand(chatId, user, messageSplit);
            case HELP -> helpCommand(chatId);
            case GET -> getCommand(chatId, username, messageSplit);
            case HISTORY -> historyCommand(chatId, user, messageSplit);
            case DEBTS -> debtsCommand(chatId, user.getId(), messageSplit);
            default -> unknownCommand(chatId);
        }
    }

    private void historyCommand(Long chatId, User user, String[] messageSplit) {
        String text;
        try {
            PageRequest page;
            if (messageSplit.length != 2) {
                page = PageRequest.of(0, 20);
            } else {
                page = PageRequest.of(Integer.parseInt(messageSplit[1]), 20);
            }
            var res = dao.findAllTransactionsRelated(chatId, user.getId(), page);
            List<String> res2 = new ArrayList<>();
            res.forEach(t -> res2.add(t.toString()));
            text = String.join("\n", res2);
        } catch (UserNotFoundException e) {
            text = "You aren't registered, try write /start";
        } catch (IllegalArgumentException e) {
            text = "Wrong format: " + e.getMessage();
        }
        telegramService.sendMessage(chatId, text);
    }

    private void startCommand(Long chatId, User user) {
        var text = """
                Well Cum to our club, %s!
                """;
        String formattedText = String.format(text, user.getUserName());
        dao.addUser(user.getId(), user.getUserName());
        telegramService.sendMessage(chatId, formattedText);
        helpCommand(chatId);
    }

    private void addCommand(Long chatId, User user, String[] messageSplit) {
        String text;
        try {
            if (messageSplit.length < 3) throw new ParseException("Wrong argument count");

            String recipient = messageSplit[1];
            // Remove '@' from the recipient username if present
            if (recipient.startsWith("@")) {
                recipient = recipient.substring(1);
            }

            String comment = "";
            if (messageSplit.length != 3) {
                comment = concatArrayExceptFirstThree(messageSplit);
            }

            Long sum = Long.parseLong(messageSplit[2]);
            var transaction = dao.addTransaction(chatId, user.getId(), recipient, sum, comment);
            text = transaction.toString();
        } catch (ParseException e) {
            text = "Wrong format: "+e.getMessage();
        } catch (UserNotFoundException e) {
            text = "User "+ e.getMessage()+ " not found";
        }
        telegramService.sendMessage(chatId, text);
    }

    private void getCommand(Long chatId, String username, String[] messageSplit) {
        String text;
        try {
            if (messageSplit.length != 2) throw new ParseException("Wrong argument count");

            String recipient = messageSplit[1];
            // Remove '@' from the recipient username if present
            if (recipient.startsWith("@")) {
                recipient = recipient.substring(1);
            }

            var debt = dao.getDebt(chatId, username, recipient);
            text = debt.toString();
        } catch (ParseException e) {
            text = "Wrong format: " + e.getMessage();
        } catch (UserNotFoundException e) {
            text = "User with name " + e.getMessage() + " isn't registered";
        }
        telegramService.sendMessage(chatId, text);
    }

    private void debtsCommand(Long chatId, Long userId, String[] messageSplit) {
        String text;
        try {
            PageRequest page;
            if (messageSplit.length != 2) {
                page = PageRequest.of(0, 20);
            } else {
                page = PageRequest.of(Integer.parseInt(messageSplit[1]), 20);
            }
            var res = dao.findAllDebtsRelated(chatId, userId, page);
            List<String> res2 = new ArrayList<>();
            res.forEach(t -> res2.add(t.toString()));
            text = String.join("\n", res2);
        } catch (UserNotFoundException e) {
            text = "User with name " + e.getMessage() + " isn't registered";
        } catch (IllegalArgumentException e) {
            text = "Wrong format: "+e.getMessage();
        }
        telegramService.sendMessage(chatId, text);
    }

    private void helpCommand(Long chatId) {
        var text = """
                /add TgUsername {sum} {comment} - adds new transaction Me->Someone with value {sum}₽
                /get TgUsername - checks size of debt between you and him
                /history {page} (0 by default) - related transactions
                /debts {page} (0 by default) - related debts
                """;
        telegramService.sendMessage(chatId, text);
    }

    private void unknownCommand(Long chatId) {
        var text = "Not recognised";
        telegramService.sendMessage(chatId, text);
    }

    private void activateSession(Long chatId, User user, String message) {
        securityService.activateSessionToken(user.getId(), message);
        String text = """
                Your session was authenticated!
                You have 1 minute ;)
                """;
        telegramService.sendMessage(chatId, text);
    }

    @Override
    public String getBotUsername() {
        return botName;
    }

    private String concatArrayExceptFirstThree(String[] words) {
        StringBuilder result = new StringBuilder();

        if (words.length > 3) {
            for (int i = 3; i < words.length; i++) {
                result.append(words[i]).append(" ");
            }
        }

        return result.toString();
    }

}

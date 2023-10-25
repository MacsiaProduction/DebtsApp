package ru.m_polukhin.debtsapp.controllers;

import org.springframework.beans.factory.annotation.Autowired;
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
    private static final String PASSWORD = "/new_password";

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
        var command = message.split(" ")[0];
        switch (command) {
            case START -> startCommand(chatId, user);
            case ADD -> addCommand(chatId, username, message);
            case HELP -> helpCommand(chatId);
            case GET -> getCommand(chatId, username, message);
            case HISTORY -> historyCommand(chatId, user);
            case DEBTS -> debtsCommand(chatId, username);
            case PASSWORD -> generatePassword(chatId, username);
            default -> unknownCommand(chatId);
        }
    }

    private void historyCommand(Long chatId, User user) {
        String text;
        try {
            var res = dao.findAllTransactionsRelated(user.getUserName());
            List<String> res2 = new ArrayList<>();
            res.forEach(t -> res2.add(t.toString()));
            text = String.join("\n", res2);
        } catch (UserNotFoundException e) {
            text = "You aren't registered, try write /start";
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

    private void addCommand(Long chatId, String username, String message) {
        String text;
        try {
            var res = message.split(" ");
            if (res.length != 3) throw new ParseException("Wrong args count");
            String recipient = res[1];
            Long sum = Long.parseLong(res[2]);
            dao.addTransaction(username, recipient, sum);
            text = "Transaction " + username + " -> " + recipient + " {" + sum + "} added";
        } catch (ParseException e) {
            text = "Wrong format: "+e.getMessage();
        } catch (UserNotFoundException e) {
            text = e.getMessage();
        }
        telegramService.sendMessage(chatId, text);
    }

    private void getCommand(Long chatId, String username, String message) {
        String text;
        try {
            var res = message.split(" ");
            if (res.length != 2) throw new ParseException("Wrong args count");
            String recipient = res[1];
            var debt = dao.getDebt(username, recipient);
            text = debt.toString();
        } catch (ParseException e) {
            text = "Wrong format: "+e.getMessage();
        } catch (UserNotFoundException e) {
            text = "User with name " + e.getMessage() + " isn't registered";
        }
        telegramService.sendMessage(chatId, text);
    }

    private void debtsCommand(Long chatId, String username) {
        String text;
        try {
            var res = dao.findAllDebtsRelated(username);
            List<String> res2 = new ArrayList<>();
            res.forEach(t -> res2.add(t.toString()));
            text = String.join("\n", res2);
        } catch (UserNotFoundException e) {
            text = "User with name " + e.getMessage() + " isn't registered";
        }
        telegramService.sendMessage(chatId, text);
    }

    private void helpCommand(Long chatId) {
        var text = """
                /add TgUsername(no @) {sum} - adds new transactions Me->Someone(без @) with value {sum} ₽
                /get TgUsername - checks size of debt
                /history - all related transactions
                /debts - all related debts
                /new_password - creates a new password for web interface
                """;
        telegramService.sendMessage(chatId, text);
    }

    private void unknownCommand(Long chatId) {
        var text = "Not recognised";
        telegramService.sendMessage(chatId, text);
    }

    private void generatePassword(Long chatId, String username) {
        String text;
        try {
            var password = securityService.generatePassword(username);
            text = """
                Your new password is: %s!
                """;
            text = String.format(text, password);
        } catch (UserNotFoundException e) {
            text = "User with name " + e.getMessage() + " isn't registered";
        }
        telegramService.sendMessage(chatId, text);
    }

    @Override
    public String getBotUsername() {
        return botName;
    }

}

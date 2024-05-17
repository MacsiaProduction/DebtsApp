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
        var username = user.getUserName();
        var messageSplit = message.split(" ");
        var command = messageSplit[0];

        switch (command) {
            case START -> {
                var len = messageSplit.length;
                if (len == 1) {
                    startCommand(chatId, threadId, user);
                } else {
                    activateSession(chatId, threadId, user, messageSplit[1]);
                }
            }
            case ADD -> addCommand(chatId, threadId, messageId, user, messageSplit);
            case HELP -> helpCommand(chatId, threadId);
            case GET -> getCommand(chatId, threadId, username, messageSplit);
            case HISTORY -> historyCommand(chatId, threadId, user, messageSplit);
            case DEBTS -> debtsCommand(chatId, threadId, user.getId(), messageSplit);
            default -> unknownCommand(chatId, threadId);
        }
    }

    private void historyCommand(Long chatId, Integer threadId, User user, String[] messageSplit) {
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
        } catch (NumberFormatException e) {
            text = "Wrong format: " + e.getMessage();
        }
        telegramService.sendMessage(chatId, threadId, text);
    }

    private void startCommand(Long chatId, Integer threadId, User user) {
        dao.addUser(1L, "test"); //todo delete

        var text = """
                Well Cum to our club, %s!
                """;
        String formattedText = String.format(text, user.getUserName());
        dao.addUser(user.getId(), user.getUserName());
        telegramService.sendMessage(chatId, threadId, formattedText);
        helpCommand(chatId, threadId);
    }

    private void addCommand(Long chatId, Integer threadId, Integer messageId, User user, String[] messageSplit) {
        try {
            if (messageSplit.length < 3) {
                throw new ParseException("Wrong argument count");
            }

            String recipient = messageSplit[1].startsWith("@") ? messageSplit[1].substring(1) : messageSplit[1];
            String comment = messageSplit.length > 3 ? concatArrayExceptFirstThree(messageSplit) : "";

            Long sum = Calculator.evaluateExpression(messageSplit[2]);
            var transaction = dao.addTransaction(chatId, user.getId(), recipient, sum, comment);
            telegramService.markAsRead(chatId, messageId);
        } catch (NumberFormatException | ParseException e) {
            telegramService.sendMessage(chatId, threadId, "Wrong format: " + e.getMessage());
        } catch (UserNotFoundException e) {
            telegramService.sendMessage(chatId, threadId, "User " + e.getMessage() + " not found");
        }
    }

    private void getCommand(Long chatId, Integer threadId, String username, String[] messageSplit) {
        String text;
        try {
            if (messageSplit.length != 2) {
                throw new ParseException("Wrong argument count");
            }

            String recipient = messageSplit[1].startsWith("@") ? messageSplit[1].substring(1) : messageSplit[1];
            var debt = dao.getDebt(chatId, username, recipient);
            text = debt.toString();
        } catch (ParseException e) {
            text = "Wrong format: " + e.getMessage();
        } catch (UserNotFoundException e) {
            text = "User with name " + e.getMessage() + " isn't registered";
        }
        telegramService.sendMessage(chatId, threadId, text);
    }

    private void debtsCommand(Long chatId, Integer threadId, Long userId, String[] messageSplit) {
        try {
            int pageNumber = 0;
            if (messageSplit.length == 2) {
                pageNumber = Integer.parseInt(messageSplit[1]);
            }
            PageRequest page = PageRequest.of(pageNumber, 20);
            var res = dao.findAllDebtsRelated(chatId, userId, page);
            String text = String.join("\n", res.map(Object::toString).toList());
            telegramService.sendMessage(chatId, threadId, text);
        } catch (UserNotFoundException e) {
            telegramService.sendMessage(chatId, threadId, "User with name " + e.getMessage() + " isn't registered");
        } catch (NumberFormatException e) {
            telegramService.sendMessage(chatId, threadId, "Wrong format: " + e.getMessage());
        }
    }

    private void helpCommand(Long chatId, Integer threadId) {
        var text = """
                /add TgUsername {sum} {comment} - adds new transaction Me->Someone with value {sum}₽
                /get TgUsername - checks size of debt between you and him
                /history {page} (0 by default) - related transactions
                /debts {page} (0 by default) - related debts
                """;
        telegramService.sendMessage(chatId, threadId, text);
    }

    private void unknownCommand(Long chatId, Integer threadId) {
        var text = "Not recognized";
        telegramService.sendMessage(chatId, threadId, text);
    }

    private void activateSession(Long chatId, Integer threadId, User user, String message) {
        securityService.activateSessionToken(user.getId(), message);
        String text = """
                Your session was authenticated!
                You have 1 minute ;)
                """;
        telegramService.sendMessage(chatId, threadId, text);
    }

    @Override
    public String getBotUsername() {
        return botName;
    }

    private String concatArrayExceptFirstThree(String[] words) {
        if (words.length > 3) {
            return String.join(" ", Arrays.copyOfRange(words, 3, words.length));
        } else {
            return "";
        }
    }


}

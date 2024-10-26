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

import static ru.m_polukhin.debtsapp.controllers.MessageUtil.*;


//todo add paying info
//todo support doubles

@Controller
public class TelegramController extends TelegramLongPollingBot {
    private final DebtsDAO dao;
    private final TelegramService telegramService;
    private final SecurityService securityService;
    private final String botName;

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
        var command = messageSplit[0].replace("@" + botName, "");
        try {
            switch (command) {
                case MessageUtil.START -> {
                    if (messageSplit.length == 1) {
                        startCommand(chatId, threadId, user);
                    } else {
                        activateSession(chatId, threadId, user, messageSplit[1]);
                    }
                }
                case MessageUtil.ADD -> addCommand(chatId, threadId, messageId, user, messageSplit);
                case MessageUtil.HELP -> helpCommand(chatId, threadId);
                case MessageUtil.GET -> getCommand(chatId, threadId, username, messageSplit);
                case MessageUtil.HISTORY -> historyCommand(chatId, threadId, user, messageSplit);
                case MessageUtil.DEBTS -> debtsCommand(chatId, threadId, user, messageSplit);
                case MessageUtil.ADD_MANY -> add2ManyCommand(chatId, threadId, messageId, user, messageSplit);
                case MessageUtil.UNDO -> undoCommand(chatId, threadId, user.getId(), messageId);
                case MessageUtil.USER_INFO -> getUserInfo(chatId, threadId, messageSplit);
                case MessageUtil.ADD_USER_INFO -> addExtraInfo(chatId, threadId, messageId, user.getId(), messageSplit);
                default -> unknownCommand(chatId, threadId);
            }
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
    }

    private void getUserInfo(Long chatId, Integer threadId, String[] messageSplit) {
        String text;
        try {
            if (messageSplit.length < 2) {
                throw new ParseException(MessageUtil.WRONG_ARGUMENT_COUNT);
            }
            String name = extractRecipient(messageSplit[1]);
            text = MessageUtil.userInfoMessage(dao.findUserByName(name));
        } catch (UserNotFoundException e) {
            text = USER_NOT_REGISTERED;
        } catch (ParseException e) {
            text = WRONG_ARGUMENT_COUNT;
        }
        telegramService.sendMessage(chatId, threadId, text, true);
    }

    private void addExtraInfo(Long chatId, Integer threadId, Integer messageId, Long userId, String[] messageSplit) {
        try {
            if (messageSplit.length < 2) {
                throw new ParseException(MessageUtil.WRONG_ARGUMENT_COUNT);
            }
            String info = concatArrayExceptFirstN(messageSplit, 2);
            if (info.length() > 100) {
                throw new ParseException("length of user info bigger then 100");
            }
            dao.updateUserExtraInfo(userId, info);
            telegramService.markAsRead(chatId, messageId);
        } catch (ParseException e) {
            telegramService.sendMessage(chatId, threadId, String.format(MessageUtil.WRONG_FORMAT, e.getMessage()));
        } catch (UserNotFoundException e) {
            telegramService.sendMessage(chatId, threadId, USER_NOT_REGISTERED);
        }
    }

    private void startCommand(Long chatId, Integer threadId, User user) {
        String formattedText = String.format(MessageUtil.WELCOME_MESSAGE, user.getUserName());
        dao.addUser(user.getId(), user.getUserName());
        telegramService.sendMessage(chatId, threadId, formattedText, true);
        helpCommand(chatId, threadId);
    }

    private void addCommand(Long chatId, Integer threadId, Integer messageId, User user, String[] messageSplit) {
        try {
            if (messageSplit.length < 3) {
                throw new ParseException(MessageUtil.WRONG_ARGUMENT_COUNT);
            }
            String recipient = extractRecipient(messageSplit[1]);
            String comment = concatArrayExceptFirstN(messageSplit, 3);
            if (comment.length() > 30) {
                throw new ParseException("length of comment bigger then 30");
            }
            Long sum = Calculator.evaluateExpression(messageSplit[2]);
            dao.addTransaction(chatId, user.getId(), recipient, sum, comment);
            telegramService.markAsRead(chatId, messageId);
        } catch (NumberFormatException | ParseException e) {
            telegramService.sendMessage(chatId, threadId, String.format(MessageUtil.WRONG_FORMAT, e.getMessage()));
        } catch (UserNotFoundException e) {
            telegramService.sendMessage(chatId, threadId, String.format(MessageUtil.USER_NOT_FOUND, e.getMessage()));
        }
    }

    private void add2ManyCommand(Long chatId, Integer threadId, Integer messageId, User user, String[] messageSplit) {
        try {
            if (messageSplit.length < 3) {
                throw new ParseException(MessageUtil.WRONG_ARGUMENT_COUNT);
            }

            List<String> recipients = new ArrayList<>();
            for (int i = 1; i < messageSplit.length - 1; i++) {
                recipients.add(extractRecipient(messageSplit[i]));
            }
            Long sum = Calculator.evaluateExpression(messageSplit[messageSplit.length - 1]);
            for (var recipient : recipients) dao.addTransaction(chatId, user.getId(), recipient, sum, "");
            telegramService.markAsRead(chatId, messageId);
        } catch (NumberFormatException | ParseException e) {
            telegramService.sendMessage(chatId, threadId, String.format(MessageUtil.WRONG_FORMAT, e.getMessage()));
        } catch (UserNotFoundException e) {
            telegramService.sendMessage(chatId, threadId, String.format(MessageUtil.USER_NOT_FOUND, e.getMessage()));
        }
    }

    private void historyCommand(Long chatId, Integer threadId, User user, String[] messageSplit) {
        String text;
        try {
            PageRequest page = messageSplit.length != 2 ? PageRequest.of(0, 20) : PageRequest.of(Integer.parseInt(messageSplit[1]), 20);
            var transactions = dao.findAllTransactionsRelated(chatId, user.getId(), page);
            StringBuilder sb = new StringBuilder("__History of *").append(user.getUserName()).append("*__\n");
            transactions.forEach(transaction -> formatTransaction(sb, transaction));
            text = sb.toString();
        } catch (UserNotFoundException e) {
            text = MessageUtil.USER_NOT_REGISTERED;
        } catch (NumberFormatException e) {
            text = String.format(MessageUtil.WRONG_FORMAT, e.getMessage());
        }
        telegramService.sendMessage(chatId, threadId, text, true);
    }

    private void getCommand(Long chatId, Integer threadId, String username, String[] messageSplit) {
        String text;
        try {
            if (messageSplit.length != 2) {
                throw new ParseException(MessageUtil.WRONG_ARGUMENT_COUNT);
            }
            String recipient = extractRecipient(messageSplit[1]);
            var debt = dao.getDebt(chatId, username, recipient);
            text = formatDebtMessage(debt, username);
        } catch (ParseException e) {
            text = String.format(MessageUtil.WRONG_FORMAT, e.getMessage());
        } catch (UserNotFoundException e) {
            text = String.format(MessageUtil.USER_NOT_FOUND, e.getMessage());
        }
        telegramService.sendMessage(chatId, threadId, text, true);
    }

    private void helpCommand(Long chatId, Integer threadId) {
        telegramService.sendMessage(chatId, threadId, MessageUtil.HELP_MESSAGE);
    }

    private void unknownCommand(Long chatId, Integer threadId) {
        telegramService.sendMessage(chatId, threadId, MessageUtil.NOT_RECOGNIZED_COMMAND);
    }

    private void debtsCommand(Long chatId, Integer threadId, User user, String[] messageSplit) {
        String text;
        try {
            PageRequest page = messageSplit.length != 2 ? PageRequest.of(0, 20) : PageRequest.of(Integer.parseInt(messageSplit[1]), 20);
            var debts = dao.findAllDebtsRelated(chatId, user.getId(), page);
            StringBuilder sb = new StringBuilder("__Debts of *").append(user.getUserName()).append("*__\n");
            debts.forEach(debt -> formatDebtMessage(sb, user.getUserName(), debt));
            text = sb.toString();
        } catch (UserNotFoundException e) {
            text = MessageUtil.USER_NOT_REGISTERED;
        }
        telegramService.sendMessage(chatId, threadId, text, true);
    }

    private void undoCommand(Long chatId, Integer threadId, Long userId, Integer messageId) {
        String text;
        try {
            var lastTransaction = dao.deleteLastTransaction(chatId, userId);
            if (lastTransaction == null) {
                throw new IllegalStateException(NO_TRANSACTIONS_YET);
            }
            text = MessageUtil.transactionUndoneMessage(lastTransaction.recipient(), lastTransaction.sum());

            telegramService.markAsRead(chatId, messageId);
        } catch (UserNotFoundException e) {
            text = MessageUtil.USER_NOT_REGISTERED;
        } catch (IllegalStateException e) {
            text = MessageUtil.NO_TRANSACTIONS_YET;
        }
        telegramService.sendMessage(chatId, threadId, text, true);
    }

    private String extractRecipient(String username) {
        return username.startsWith("@") ? username.substring(1) : username;
    }

    private String concatArrayExceptFirstN(String[] array, Integer from) {
        if (array.length <= from) return "";
        return String.join(" ", Arrays.copyOfRange(array, from, array.length));
    }

    private void activateSession(Long chatId, Integer threadId, User user, String message) {
        securityService.activateSessionToken(user.getId(), message);
        telegramService.sendMessage(chatId, threadId, SESSION_AUTHENTICATED);
    }

    @Override
    public String getBotUsername() {
        return this.botName;
    }
}

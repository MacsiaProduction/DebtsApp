package ru.m_polukhin.debtsapp.controllers;

import org.apache.commons.lang3.StringUtils;
import ru.m_polukhin.debtsapp.dto.DebtInfo;
import ru.m_polukhin.debtsapp.dto.TransactionInfo;
import ru.m_polukhin.debtsapp.models.UserData;

public class MessageUtil {
    // Commands
    public static final String START = "/start";
    public static final String ADD = "/add";
    public static final String ADD_MANY = "/add_many";
    public static final String GET = "/get";
    public static final String HISTORY = "/history";
    public static final String HELP = "/help";
    public static final String DEBTS = "/debts";
    public static final String UNDO = "/undo_last";
    public static final String USER_INFO = "/get_info";
    public static final String ADD_USER_INFO = "/add_info";
    public static final String SUMMARY = "/summary";
    public static final String UPDATE_NICKNAME = "/update_nickname";

    // Messages
    public static final String WELCOME_MESSAGE = "Well Cum to our club, *%s*!";
    public static final String NICKNAME_UPDATE_MESSAGE = "Your nickname was updated, *%s*!";
    public static final String HELP_MESSAGE = """
            /add TgUsername {sum} {comment} - adds new transaction Me->Someone with value {sum}₽
            /add_many TgUsername1 TgUsername2 ... {sum} - adds many transactions
            /get TgUsername - checks size of debt between you and him
            /history {page} (0 by default) - related transactions
            /debts {page} (0 by default) - related debts
            /undo_last - reverts last transaction made by you
            /add_info - adds extra info
            /get_info TgUsername - gets extra info of target user
            /summary - entire chat's debts summary
            /update_nickname - updates your nickname in the bot.
            """;
    public static final String NOT_RECOGNIZED_COMMAND = "Not recognized";
    public static final String WRONG_ARGUMENT_COUNT = "Wrong argument count";
    public static final String WRONG_FORMAT = "Wrong format: %s";
    public static final String USER_NOT_FOUND = "User %s not found";
    public static final String USER_NOT_REGISTERED = "You aren't registered, try write /start";
    public static final String NO_TRANSACTIONS_YET = "You don't have transactions yet";

    // Formatted message templates
    public static String transactionUndoneMessage(String recipient, Long sum) {
        return String.format("*Your* transaction to *%s*, *%d*₽ was undone", recipient, sum);
    }

    public static String userInfoMessage(UserData userData) {
        if (StringUtils.isEmpty(userData.extraInfo())) {
            return "No extra info provided";
        }
        return String.format("*%s* info:\n%s", userData.telegramName(), userData.extraInfo());
    }

    public static String formatDebtMessage(String from, String to, Long sum) {
        return String.format("*%s* owes *%s* %d₽\n", to, from, sum);
    }

    public static String formatDebtMessage(DebtInfo debt, String username) {
        String from = debt.sum() > 0 ? debt.from() : debt.to();
        String to = debt.sum() > 0 ? debt.to() : debt.from();
        from = (from.equals(username)) ? "You" : from;
        to = (to.equals(username)) ? "You" : to;
        return formatDebtMessage(from, to, Math.abs(debt.sum()));
    }

    public static String oweMessageWithDog(DebtInfo debt) {
        String from = debt.sum() > 0 ? debt.from() : debt.to();
        String to = debt.sum() > 0 ? debt.to() : debt.from();
        return String.format("@%s owes *%s* %d₽\n", to, from, Math.abs(debt.sum()));
    }

    public static void formatDebtMessage(StringBuilder sb, DebtInfo debt) {
        String from = debt.sum() > 0 ? debt.from() : debt.to();
        String to = debt.sum() > 0 ? debt.to() : debt.from();
        sb.append(formatDebtMessage(from, to, Math.abs(debt.sum())));
    }

    public static void formatTransaction(StringBuilder sb, TransactionInfo transaction) {
        String comment = transaction.comment() != null ? transaction.comment() : "";
        sb.append(String.format(
                "- *%s* transfer *%s* *%d₽*: %s\n",
                transaction.sender(),
                transaction.recipient(),
                transaction.sum(),
                comment));
    }

    public static void formatDebtMessage(StringBuilder sb, String username, DebtInfo debt) {
        sb.append(MessageUtil.formatDebtMessage(debt, username));
    }
}
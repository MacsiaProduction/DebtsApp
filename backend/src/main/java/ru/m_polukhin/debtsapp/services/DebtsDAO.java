package ru.m_polukhin.debtsapp.services;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import ru.m_polukhin.debtsapp.dto.DebtInfo;
import ru.m_polukhin.debtsapp.dto.TransactionInfo;
import ru.m_polukhin.debtsapp.exceptions.ParseException;
import ru.m_polukhin.debtsapp.exceptions.UserNotFoundException;
import ru.m_polukhin.debtsapp.exceptions.UserNotFoundUnchecked;
import ru.m_polukhin.debtsapp.models.ActiveSessionToken;
import ru.m_polukhin.debtsapp.models.Debt;
import ru.m_polukhin.debtsapp.models.Transaction;
import ru.m_polukhin.debtsapp.models.UserData;
import ru.m_polukhin.debtsapp.repository.SessionRepository;
import ru.m_polukhin.debtsapp.repository.TransactionRepository;
import ru.m_polukhin.debtsapp.repository.UserRepository;
import ru.m_polukhin.debtsapp.utils.CustomPageImpl;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DebtsDAO {
    private final PasswordEncoder passwordEncoder;
    private final TransactionRepository transactionRepository;
    private final DebtGraphService debtGraphService;
    private final UserRepository userRepository;
    private final SessionRepository sessionRepository;

    public TransactionInfo addTransaction(Long chatId, Long senderId, String recipient, Long sum, String comment) throws UserNotFoundException, ParseException {
        Long recipientId = getIdByName(recipient);
        var transaction = new Transaction(sum, senderId, recipientId, chatId, comment);
        transactionRepository.save(transaction);
        debtGraphService.increaseDebt(senderId, recipientId, sum, chatId);
        return coverTransaction(transaction);
    }

    public UserData addTelegramUser(Long telegramId, String telegramName) {
        return userRepository.findByTelegramId(telegramId).orElseGet(() -> {
            var user = new UserData(null, telegramName, telegramId, null, null);
            return userRepository.save(user);
        });
    }

    public Page<TransactionInfo> findAllTransactionsFromTo(Long chatId, String sender, String recipient, Pageable pageable) throws UserNotFoundException {
        var transactions = transactionRepository.findAllByChatIdAndSenderIdAndRecipientId(chatId, getIdByName(sender), getIdByName(recipient), pageable);
        return coverTransactions(transactions);
    }

    public Page<TransactionInfo> findAllTransactionsFromTo(String sender, String recipient, Pageable pageable) throws UserNotFoundException {
        var transactions = transactionRepository.findAllBySenderIdAndRecipientId(getIdByName(sender), getIdByName(recipient), pageable);
        return coverTransactions(transactions);
    }

    public Page<TransactionInfo> findAllTransactionsRelated(Long chatId, Long userId, Pageable pageable) throws UserNotFoundException {
        return coverTransactions(transactionRepository.findAllByChatIdAndSenderIdOrChatIdAndRecipientId(chatId, userId, chatId, userId, pageable));
    }

    public Page<TransactionInfo> findAllTransactionsRelated(Long userId, Pageable pageable) throws UserNotFoundException {
        return coverTransactions(transactionRepository.findAllBySenderIdOrRecipientId(userId, userId, pageable));
    }

    public DebtInfo getDebt(Long chatId, String sender, String receiver) throws UserNotFoundException {
        Long senderId = getIdByName(sender);
        Long receiverId = getIdByName(receiver);
        var debt = debtGraphService.getDebtBetweenUsers(senderId, receiverId, chatId);
        if (debt.isEmpty()) return new DebtInfo(sender, receiver, 0L, chatId);
        Debt d = debt.get();
        return new DebtInfo(getNameById(d.getSenderId()), getNameById(d.getRecipientId()), d.getAmount(), chatId);
    }

    public Page<DebtInfo> findAllDebtsRelated(Long userId, Pageable pageable) throws UserNotFoundException {
        return coverDebts(debtGraphService.findAllDebtsRelated(userId, pageable));
    }

    public Page<DebtInfo> findAllDebtsRelated(Long chatId, Long userId, Pageable pageable) throws UserNotFoundException {
        return coverDebts(debtGraphService.findAllDebtsRelated(chatId, userId, pageable));
    }

    public UserData findUserByName(String username) throws UserNotFoundException {
        return userRepository.findByTelegramName(username)
                .orElseThrow(() -> new UserNotFoundException(username));
    }

    public java.util.Optional<UserData> findUserByUsername(String username) {
        return userRepository.findByUsername(username);
    }

    public UserData findUserById(Long id) throws UserNotFoundException {
        return userRepository.findById(id)
                .orElseThrow(() -> new UserNotFoundException(id));
    }

    public Long getIdByName(String username) throws UserNotFoundException {
        return findUserByName(username).getId();
    }

    public Long getIdByTelegramId(Long telegramId) throws UserNotFoundException {
        return userRepository.findByTelegramId(telegramId)
                .map(UserData::getId)
                .orElseThrow(() -> new UserNotFoundException(telegramId));
    }

    public void addActiveSession(ActiveSessionToken activeSessionToken) {
        sessionRepository.insertSessionToken(activeSessionToken.userId(), activeSessionToken.hash(), activeSessionToken.expirationDate());
    }

    public ActiveSessionToken getActiveSession(String sessionToken) throws UserNotFoundException {
        for (var token : sessionRepository.findAll()) {
            if (passwordEncoder.matches(sessionToken, token.hash())) {
                return token;
            }
        }
        throw new UserNotFoundException(sessionToken);
    }

    public ActiveSessionToken getUsersSession(Long userId) throws UserNotFoundException {
        return sessionRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));
    }

    public Page<DebtInfo> getAllDebtsInChat(Long chatId, Pageable page) throws UserNotFoundException {
        return coverDebts(debtGraphService.findAllDebtsInChat(chatId, page));
    }

    public List<Long> getAllChats() {
        return debtGraphService.findAllUniqueChatIds();
    }

    public void deleteChatHistory(Long chatId) {
        debtGraphService.deleteAllByChatId(chatId);
        transactionRepository.deleteAllByChatId(chatId);
    }

    private String getNameById(Long id) throws UserNotFoundException {
        return findUserById(id).getTelegramName();
    }

    private Page<TransactionInfo> coverTransactions(Page<Transaction> transactions) throws UserNotFoundException {
        try {
            return new CustomPageImpl<>(transactions.stream().map(t -> {
                try {
                    return coverTransaction(t);
                } catch (UserNotFoundException e) {
                    throw new UserNotFoundUnchecked(e.getMessage());
                }
            }).collect(Collectors.toList()));
        } catch (UserNotFoundUnchecked e) {
            throw new UserNotFoundException(e.getMessage());
        }
    }

    private Page<DebtInfo> coverDebts(List<Debt> debts) throws UserNotFoundException {
        try {
            return new CustomPageImpl<>(debts.stream().map(debt -> {
                try {
                    return new DebtInfo(getNameById(debt.getSenderId()),
                            getNameById(debt.getRecipientId()),
                            debt.getAmount(),
                            debt.getChatId());
                } catch (UserNotFoundException e) {
                    throw new UserNotFoundUnchecked(e.getMessage());
                }
            }).collect(Collectors.toList()));
        } catch (UserNotFoundUnchecked e) {
            throw new UserNotFoundException(e.getMessage());
        }
    }

    private TransactionInfo coverTransaction(Transaction transaction) throws UserNotFoundException {
        return new TransactionInfo(
                getNameById(transaction.getSenderId()),
                getNameById(transaction.getRecipientId()),
                transaction.getSum(),
                transaction.getChatId(),
                transaction.getComment()
        );
    }
}

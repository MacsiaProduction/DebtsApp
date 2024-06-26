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
import ru.m_polukhin.debtsapp.repository.DebtRepository;
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
    private final DebtRepository debtRepository;
    private final UserRepository userRepository;
    private final SessionRepository sessionRepository;

    public TransactionInfo addTransaction(Long chatId, Long senderId, String recipient, Long sum, String comment) throws UserNotFoundException, ParseException {
        Long recipientId = getIdByName(recipient);
        var transaction = new Transaction(sum, senderId, recipientId, chatId, comment);
        transactionRepository.save(transaction);
        debtRepository.increaseDebt(senderId, recipientId, sum, chatId);
        return coverTransaction(transaction);
    }

    //todo update nickname
    public void addUser(Long userId, String username) {
        if(!userRepository.existsById(userId)) {
            userRepository.insertUser(userId, username);
        }
    }

    public Page<TransactionInfo> findAllTransactionsFromTo(Long chatId, String sender, String recipient, Pageable pageable) throws UserNotFoundException {
        var transactions = transactionRepository.findAllByChatIdAndSenderIdAndRecipientId(chatId, getIdByName(sender), getIdByName(recipient), pageable);
        try {
            return coverTransactions(transactions);
        } catch (UserNotFoundUnchecked e) {
            throw new UserNotFoundException(e.getMessage());
        }
    }

    public Page<TransactionInfo> findAllTransactionsFromTo(String sender, String recipient, Pageable pageable) throws UserNotFoundException {
        var transactions = transactionRepository.findAllBySenderIdAndRecipientId(getIdByName(sender), getIdByName(recipient), pageable);
        try {
            return coverTransactions(transactions);
        } catch (UserNotFoundUnchecked e) {
            throw new UserNotFoundException(e.getMessage());
        }
    }

    public Page<TransactionInfo> findAllTransactionsRelated(Long chatId, Long userId, Pageable pageable) throws UserNotFoundException {
        Page<Transaction> transactions = transactionRepository.findAllByChatIdAndSenderIdOrChatIdAndRecipientId(chatId, userId, chatId, userId, pageable);
        try {
            return coverTransactions(transactions);
        } catch (UserNotFoundUnchecked e) {
            throw new UserNotFoundException(e.getMessage());
        }
    }

    public Page<TransactionInfo> findAllTransactionsRelated(Long userId, Pageable pageable) throws UserNotFoundException {
        Page<Transaction> transactions = transactionRepository.findAllBySenderIdOrRecipientId(userId, userId, pageable);
        try {
            return coverTransactions(transactions);
        } catch (UserNotFoundUnchecked e) {
            throw new UserNotFoundException(e.getMessage());
        }
    }

    public DebtInfo getDebt(Long chatId, String sender, String receiver) throws UserNotFoundException {
        Debt debt = debtRepository.getDebtBetweenUsers(getIdByName(sender), getIdByName(receiver), chatId);
        if (debt == null) return new DebtInfo(sender, receiver, 0L, chatId);
        return new DebtInfo(getNameById(debt.getSenderId()),
                            getNameById(debt.getRecipientId()),
                            debt.getSum(),
                            chatId);
    }

    public Page<DebtInfo> getAllDebtsInChat(Long chatId, Pageable page) {
        try {
            return coverDebts(debtRepository.findByChatId(chatId, page));
        } catch (UserNotFoundUnchecked e) {
            throw new UserNotFoundUnchecked(e.getMessage());
        }
    }

    public Page<DebtInfo> findAllDebtsRelated(Long userId, Pageable pageable) throws UserNotFoundException {
        try {
            return coverDebts(debtRepository.findAllDebtsRelated(userId, pageable));
        } catch (UserNotFoundUnchecked e) {
            throw new UserNotFoundException(e.getMessage());
        }
    }

    public Page<DebtInfo> findAllDebtsRelated(Long chatId, Long userId, Pageable pageable) throws UserNotFoundException {
        try {
            return coverDebts(debtRepository.findAllDebtsRelated(chatId, userId, pageable));
        } catch (UserNotFoundUnchecked e) {
            throw new UserNotFoundException(e.getMessage());
        }
    }

    public UserData findUserByName(String username) throws UserNotFoundException {
        var userOptional = userRepository.findByTelegramName(username);
        if(userOptional.isEmpty()) throw new UserNotFoundException(username);
        return userOptional.get();
    }

    public void addActiveSession(ActiveSessionToken activeSessionToken) {
        sessionRepository.insertSessionToken(activeSessionToken.userId(), activeSessionToken.hash(), activeSessionToken.expirationDate());
    }

    //todo give a client some extra number to optimize search in db
    public ActiveSessionToken getActiveSession(String sessionToken) throws UserNotFoundException {
        var tokens = sessionRepository.findAll();
        for(var token: tokens) {
            if (passwordEncoder.matches(sessionToken, token.hash())) {
                return token;
            }
        }
        throw new UserNotFoundException(sessionToken);
    }

    public ActiveSessionToken getUsersSession(Long userId) throws UserNotFoundException {
        var token = sessionRepository.findById(userId);
        if (token.isEmpty()) throw new UserNotFoundException(userId);
        return token.get();
    }

    public Long getIdByName(String username) throws UserNotFoundException {
        return findUserByName(username).id();
    }

    public List<Long> getAllChats() {
        return debtRepository.findAllUniqueChatIds();
    }

    public void deleteChatHistory(Long chatId) {
        debtRepository.deleteAllByChatId(chatId);
        transactionRepository.deleteAllByChatId(chatId);
    }

    private String getNameById(Long id) throws UserNotFoundException {
        var userInfo = userRepository.findById(id);
        if (userInfo.isEmpty()) throw new UserNotFoundException(id);
        return userInfo.get().telegramName();
    }

    private Page<TransactionInfo> coverTransactions(Page<Transaction> transactions) throws UserNotFoundUnchecked {
        return new CustomPageImpl<>(transactions.stream().map(transaction -> {
            try {
                return coverTransaction(transaction);
            } catch (UserNotFoundException e) {
                throw new UserNotFoundUnchecked(e.getMessage());
            }
        }).collect(Collectors.toList()));
    }

    //todo maybe not page
    private Page<DebtInfo> coverDebts(List<Debt> debts) throws UserNotFoundUnchecked {
        return new CustomPageImpl<>(debts.stream().map(debt -> {
            try {
                return new DebtInfo(getNameById(debt.getSenderId()),
                                    getNameById(debt.getRecipientId()),
                                    debt.getSum(),
                                    debt.getChatId());
            } catch (UserNotFoundException e) {
                throw new UserNotFoundUnchecked(e.getMessage());
            }
        }).collect(Collectors.toList()));
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

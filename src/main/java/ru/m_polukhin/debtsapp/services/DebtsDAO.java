package ru.m_polukhin.debtsapp.services;

import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import ru.m_polukhin.debtsapp.dto.DebtInfo;
import ru.m_polukhin.debtsapp.dto.TransactionInfo;
import ru.m_polukhin.debtsapp.models.*;
import ru.m_polukhin.debtsapp.repository.DebtRepository;
import ru.m_polukhin.debtsapp.repository.SessionRepository;
import ru.m_polukhin.debtsapp.repository.TransactionRepository;
import ru.m_polukhin.debtsapp.repository.UserRepository;
import ru.m_polukhin.debtsapp.exceptions.ParseException;
import ru.m_polukhin.debtsapp.exceptions.UserNotFoundException;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class DebtsDAO {
    private final PasswordEncoder passwordEncoder;

    private final TransactionRepository transactionRepository;
    private final DebtRepository debtRepository;
    private final UserRepository userRepository;
    private final SessionRepository sessionRepository;

    public void addTransaction(String sender, String recipient, Long sum) throws UserNotFoundException, ParseException {
        Long senderId = getIdByName(sender);
        Long recipientId = getIdByName(recipient);
        var transaction = new Transaction(sum, senderId, recipientId);
        transactionRepository.save(transaction);
        debtRepository.increaseDebt(senderId, recipientId, sum);
    }

    //todo update nickname
    public void addUser(Long userId, String username) {
        if(!userRepository.existsById(userId)) {
            userRepository.save(new UserData(userId, username));
        }
    }

    //todo page
    public List<TransactionInfo> findAllTransactionsFromTo(String sender, String recipient) throws UserNotFoundException {
        var res = transactionRepository.findAllBySenderIdAndRecipientId(getIdByName(sender),getIdByName(recipient));
        return coverTransactions(res);
    }

    public List<TransactionInfo> findAllTransactionsRelated(Long userId) throws UserNotFoundException {
        List<Transaction> transactions = transactionRepository.findAllBySenderIdOrRecipientId(userId,userId);
        return coverTransactions(transactions);
    }

    public DebtInfo getDebt(String sender, String receiver) throws UserNotFoundException {
        Debt debt = debtRepository.getDebtBetweenUsers(getIdByName(sender), getIdByName(receiver));
        if (debt == null) return new DebtInfo(sender, receiver, 0L);
        return new DebtInfo(getNameById(debt.getSenderId()),
                            getNameById(debt.getRecipientId()),
                            debt.getSum());
    }

    public List<DebtInfo> findAllDebtsRelated(String username) throws UserNotFoundException {
        return coverDebts(debtRepository.findAllDebtsRelated(getIdByName(username)));
    }

    public UserData findUserByName(String username) throws UserNotFoundException {
        var userOptional = userRepository.findByTelegramName(username);
        if(userOptional.isEmpty()) throw new UserNotFoundException(username);
        return userOptional.get();
    }

    public void addActiveSession(ActiveSessionToken activeSessionToken) {
        sessionRepository.save(activeSessionToken);
    }

    //todo give a client some extra number to not search in db
    public ActiveSessionToken getActiveSession(String sessionToken) throws UserNotFoundException {
        var tokens = sessionRepository.findAll();
        for(var token: tokens) {
            if (passwordEncoder.matches(sessionToken, token.getHash())) {
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
        return findUserByName(username).getId();
    }

    private String getNameById(Long id) throws UserNotFoundException {
        var userInfo = userRepository.findById(id);
        if (userInfo.isEmpty()) throw new UserNotFoundException(id);
        return userInfo.get().getTelegramName();
    }

    private List<TransactionInfo> coverTransactions(Iterable<Transaction> transactions) throws UserNotFoundException {
        var list = new ArrayList<TransactionInfo>();
        for (var t : transactions) {
            list.add(new TransactionInfo(getNameById(t.getSenderId()), getNameById(t.getRecipientId()), t.getSum()));
        }
        return list;
    }

    private List<DebtInfo> coverDebts(Iterable<Debt> debts) throws UserNotFoundException {
        var list = new ArrayList<DebtInfo>();
        for (var t : debts) {
            list.add(new DebtInfo(getNameById(t.getSenderId()), getNameById(t.getRecipientId()), t.getSum()));
        }
        return list;
    }
}

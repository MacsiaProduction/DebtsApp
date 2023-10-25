package ru.m_polukhin.debtsapp.services;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import ru.m_polukhin.debtsapp.models.*;
import ru.m_polukhin.debtsapp.repository.DebtRepository;
import ru.m_polukhin.debtsapp.repository.TransactionRepository;
import ru.m_polukhin.debtsapp.repository.UserRepository;
import ru.m_polukhin.debtsapp.exceptions.ParseException;
import ru.m_polukhin.debtsapp.exceptions.UserNotFoundException;

import java.util.ArrayList;
import java.util.List;

@Service
public class DebtsDAO {
    TransactionRepository transactionRepository;
    DebtRepository debtRepository;
    UserRepository userRepository;

    @Autowired
    private DebtsDAO(TransactionRepository repository, DebtRepository debtRepository, UserRepository userRepository) {
        this.transactionRepository = repository;
        this.debtRepository = debtRepository;
        this.userRepository = userRepository;
    }

    public void addTransaction(String sender, String recipient, Long sum) throws UserNotFoundException, ParseException {
        Long senderId = getIdByName(sender);
        Long recipientId = getIdByName(recipient);
        var transaction = new Transaction(sum, senderId, recipientId);
        transactionRepository.save(transaction);
        debtRepository.increaseDebt(senderId, recipientId, sum);
    }

    public void addUser(Long userId, String username) {
        if(userRepository.findById(userId).isEmpty()) {
            userRepository.save(new UserData(userId, username));
        }
    }

    public List<TransactionInfo> findAllTransactionsFromTo(String sender, String recipient) throws UserNotFoundException {
        var res = transactionRepository.findAllBySenderIdAndRecipientId(getIdByName(sender),getIdByName(recipient));
        return coverTransactions(res);
    }

    public List<TransactionInfo> findAllTransactionsRelated(String username) throws UserNotFoundException {
        Long userId = getIdByName(username);
        List<Transaction> transactions = transactionRepository.findAllBySenderIdOrRecipientId(userId,userId);
        return coverTransactions(transactions);
    }

    public List<TransactionInfo> findAllTransactions() {
        var res = transactionRepository.findAll();
        return coverTransactions(res);
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

    public List<DebtInfo> findAllDebts() {
        return coverDebts(debtRepository.findAll());
    }

    public UserData findUserByName(String username) throws UserNotFoundException {
        var user = userRepository.findByTelegramName(username);
        if (user == null) throw new UserNotFoundException(username);
        return user;
    }

    public void changeUserPassword(Long userId, String newPasswordHashed) throws UserNotFoundException {
        var userOptional = userRepository.findById(userId);
        if(userOptional.isEmpty()) throw new UserNotFoundException("User with id " + userId + "wasn't found");
        UserData user = userOptional.get();
        userRepository.save(new UserData(userId, user.getTelegramName(), newPasswordHashed));
    }

    private Long getIdByName(String username) throws UserNotFoundException {
        var user = userRepository.findByTelegramName(username);
        if (user == null) throw new UserNotFoundException(username);
        return userRepository.findByTelegramName(username).getId();
    }

    private String getNameById(Long id) throws UserNotFoundException {
        var userInfo = userRepository.findById(id);
        if (userInfo.isEmpty()) throw new UserNotFoundException(id);
        return userInfo.get().getTelegramName();
    }

    private List<TransactionInfo> coverTransactions(Iterable<Transaction> transactions){
        var list = new ArrayList<TransactionInfo>();
        try {
            for (var t : transactions) {
                list.add(new TransactionInfo(getNameById(t.getSenderId()), getNameById(t.getRecipientId()), t.getSum()));
            }
        } catch (UserNotFoundException e) {
            throw new RuntimeException(e);
        }
        return list;
    }

    private List<DebtInfo> coverDebts(Iterable<Debt> debts) {
        var list = new ArrayList<DebtInfo>();
        try {
            for (var t : debts) {
                list.add(new DebtInfo(getNameById(t.getSenderId()), getNameById(t.getRecipientId()), t.getSum()));
            }
        } catch (UserNotFoundException e) {
            throw new RuntimeException(e);
        }
        return list;
    }
}

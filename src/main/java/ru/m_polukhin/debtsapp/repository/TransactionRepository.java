package ru.m_polukhin.debtsapp.repository;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;
import ru.m_polukhin.debtsapp.models.Transaction;

import java.util.List;

@Repository
public interface TransactionRepository extends CrudRepository<Transaction, Long> {
    List<Transaction> findAllBySenderIdOrRecipientId(Long senderId, Long recipientId);
    List<Transaction> findAllBySenderIdAndRecipientId(Long senderId, Long recipientId);
}

package ru.m_polukhin.debtsapp.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;
import ru.m_polukhin.debtsapp.models.Transaction;

@Repository
public interface TransactionRepository extends CrudRepository<Transaction, Long> {
    Page<Transaction> findAllByChatIdAndSenderIdOrChatIdAndRecipientId(Long chatId, Long senderId, Long chatId2, Long recipientId, Pageable pageable);
    Page<Transaction> findAllByChatIdAndSenderIdAndRecipientId(Long chatId, Long senderId, Long recipientId, Pageable pageable);
    Page<Transaction> findAllBySenderIdAndRecipientId(Long senderId, Long recipientId, Pageable pageable);
    Page<Transaction> findAllBySenderIdOrRecipientId(Long senderId, Long ChatId, Pageable pageable);
    void deleteAllByChatId(Long chatId);
}

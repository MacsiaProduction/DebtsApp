package ru.m_polukhin.debtsapp.repository;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jdbc.repository.query.Modifying;
import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import ru.m_polukhin.debtsapp.models.Debt;

import java.util.List;

@Repository
public interface DebtRepository extends CrudRepository<Debt, Long> {
    //TODO Neo4j
    @Modifying
    @Transactional
    @Query(value =
            "INSERT INTO debts (sender_id, recipient_id, sum, chat_id) " +
                    "VALUES (LEAST(:senderId, :recipientId), GREATEST(:senderId, :recipientId), " +
                    "        CASE WHEN :senderId < :recipientId THEN :sum ELSE -1 * :sum END, :chatId) " +
                    "ON CONFLICT (sender_id, recipient_id, chat_id) " +
                    "DO UPDATE SET sum = debts.sum + " +
                    "    CASE WHEN :senderId < :recipientId THEN :sum ELSE -1 * :sum END ")
    void increaseDebt(@Param("senderId") Long senderId,
                      @Param("recipientId") Long recipientId,
                      @Param("sum") Long sum,
                      @Param("chatId") Long chatId);

    @Transactional(readOnly = true)
    @Query("SELECT d " +
            "FROM debts d " +
            "WHERE d.chat_id = :chatId " +
            "AND ((d.recipient_id = :senderId AND d.recipient_id = :recipientId) " +
            "OR (d.recipient_id = :recipientId AND d.recipient_id = :senderId))")
    Debt getDebtBetweenUsers(@Param("senderId") Long senderId,
                             @Param("recipientId") Long recipientId,
                             @Param("chatId") Long chatId);

    @Transactional(readOnly = true)
    @Query("SELECT d " +
            "FROM debts d " +
            "WHERE ((d.sender_id = :senderId AND d.recipient_id = :recipientId) " +
            "OR (d.recipient_id = :recipientId AND d.recipient_id = :senderId))")
    Debt getDebtBetweenUsers(@Param("senderId") Long senderId,
                             @Param("recipientId") Long recipientId);


    @Transactional(readOnly = true)
    @Query("SELECT d " +
            "FROM debts d " +
            "WHERE (d.recipient_id = :id OR d.recipient_id = :id)"+
            "ORDER BY d.sum DESC")
    List<Debt> findAllDebtsRelated(@Param("id") Long id, Pageable pageable);

    @Transactional(readOnly = true)
    @Query("SELECT d " +
            "FROM debts d " +
            "WHERE ((d.recipient_id = :id OR d.recipient_id = :id) AND (d.chat_id= :chatId))"+
            "ORDER BY d.sum DESC")
    List<Debt> findAllDebtsRelated(@Param("chatId") Long chatId, @Param("id") Long id, Pageable pageable);

    @Transactional(readOnly = true)
    @Query("SELECT DISTINCT d.chat_id FROM debts d")
    List<Long> findAllUniqueChatIds();

    @Transactional(readOnly = true)
    @Query("SELECT d FROM debts d WHERE d.chat_id = :chatId ORDER BY d.sum DESC")
    List<Debt> findByChatId(@Param("chatId") Long chatId, Pageable pageable);

    @Transactional
    @Modifying
    @Query("DELETE FROM debts d WHERE d.chat_id = :chatId")
    void deleteAllByChatId(@Param("chatId") Long chatId);
}

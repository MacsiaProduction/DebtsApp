package ru.m_polukhin.debtsapp.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import ru.m_polukhin.debtsapp.models.Debt;

import java.util.List;

@Repository
public interface DebtRepository extends CrudRepository<Debt, Long> {
    //TODO delete rows with zero
    @Modifying
    @Transactional
    @Query(value =
            "INSERT INTO debts (sender_id, recipient_id, sum, chat_id) " +
                    "VALUES (LEAST(:senderId, :recipientId), GREATEST(:senderId, :recipientId), " +
                    "        CASE WHEN :senderId < :recipientId THEN :sum ELSE -1 * :sum END, :chatId) " +
                    "ON CONFLICT (sender_id, recipient_id, chat_id) " +
                    "DO UPDATE SET sum = debts.sum + " +
                    "    CASE WHEN :senderId < :recipientId THEN :sum ELSE -1 * :sum END ",
            nativeQuery = true)
    void increaseDebt(@Param("senderId") Long senderId,
                      @Param("recipientId") Long recipientId,
                      @Param("sum") Long sum,
                      @Param("chatId") Long chatId);

    @Transactional(readOnly = true)
    @Query("SELECT d " +
            "FROM Debt d " +
            "WHERE ((d.id.senderId = :senderId AND d.id.recipientId = :recipientId) OR " +
            "(d.id.senderId = :recipientId AND d.id.recipientId = :senderId)) " +
            "AND d.id.chatId = :chatId")
    Debt getDebtBetweenUsers(@Param("senderId") Long senderId,
                             @Param("recipientId") Long recipientId,
                             @Param("chatId") Long chatId);


    @Transactional(readOnly = true)
    @Query("SELECT d " +
            "FROM Debt d " +
            "WHERE (d.id.senderId = :id OR d.id.recipientId = :id)"+
            "ORDER BY d.sum DESC")
    Page<Debt> findAllDebtsRelated(@Param("id") Long id, Pageable pageable);

    @Transactional(readOnly = true)
    @Query("SELECT d " +
            "FROM Debt d " +
            "WHERE ((d.id.senderId = :id OR d.id.recipientId = :id) AND (d.id.chatId = :chatId))"+
            "ORDER BY d.sum DESC")
    Page<Debt> findAllDebtsRelated(@Param("chatId") Long chatId, @Param("id") Long id, Pageable pageable);

    @Query("SELECT DISTINCT d.id.chatId FROM Debt d WHERE d.sum <> 0")
    List<Long> findAllUniqueChatIds();

    @Query("SELECT d FROM Debt d WHERE d.id.chatId = :chatId ORDER BY d.sum DESC")
    Page<Debt> findByChatId(@Param("chatId") Long chatId, Pageable pageable);
}

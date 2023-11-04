package ru.m_polukhin.debtsapp.repository;

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
    @Modifying
    @Transactional
    @Query(value =
            "INSERT INTO debts (sender_id, recipient_id, sum) " +
                    "VALUES (:senderId, :recipientId, :sum) " +
                    "ON CONFLICT (LEAST(sender_id, recipient_id), GREATEST(sender_id, recipient_id)) " +
                    "DO UPDATE SET sum = CASE " +
                    "  WHEN (debts.sender_id = :senderId AND debts.recipient_id = :recipientId) THEN debts.sum + :sum " +
                    "  WHEN (debts.sender_id = :recipientId AND debts.recipient_id = :senderId) THEN debts.sum - :sum " +
                    "ELSE debts.sum " +
                    "END",
            nativeQuery = true)
    void increaseDebt(@Param("senderId") Long senderId, @Param("recipientId") Long recipientId, @Param("sum") Long sum);

    @Transactional(readOnly = true)
    @Query("SELECT d " +
            "FROM Debt d " +
            "WHERE (d.id.senderId = :senderId AND d.id.recipientId = :recipientId) OR " +
            "(d.id.senderId = :recipientId AND d.id.recipientId = :senderId)")
    Debt getDebtBetweenUsers(@Param("senderId") Long senderId, @Param("recipientId") Long recipientId);

    @Transactional(readOnly = true)
    @Query("SELECT d " +
            "FROM Debt d " +
            "WHERE (d.id.senderId = :id OR d.id.recipientId = :id)")
    List<Debt> findAllDebtsRelated(@Param("id") Long id);
}

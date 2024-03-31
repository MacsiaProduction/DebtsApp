package ru.m_polukhin.debtsapp.repository;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.data.jdbc.DataJdbcTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import ru.m_polukhin.debtsapp.models.Debt;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@Testcontainers
@DataJdbcTest
public class DebtRepositoryTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>(
            "postgres:15-alpine"
    );

    @Autowired
    private DebtRepository debtRepository;

    @Test
    public void connectionEstablished() {
        assertThat(postgres.isCreated()).isTrue();
        assertThat(postgres.isRunning()).isTrue();
    }

    @Test
    public void testIncreaseDebt() {
        Long senderId = 1L;
        Long recipientId = 2L;
        Long sum = 100L;
        Long chatId = 123L;

        debtRepository.increaseDebt(senderId, recipientId, sum, chatId);

        Debt debt = debtRepository.getDebtBetweenUsers(senderId, recipientId, chatId);
        assertNotNull(debt);
        assertEquals(sum, debt.getSum());
        assertEquals(senderId, debt.getSenderId());
        assertEquals(recipientId, debt.getRecipientId());
        assertEquals(chatId, debt.getChatId());
    }

    @Test
    public void testInverseDebt() {
        Long senderId = 1L;
        Long recipientId = 2L;
        long sum = 100L;
        Long chatId = 123L;

        debtRepository.increaseDebt(senderId, recipientId, sum, chatId);
        debtRepository.increaseDebt(recipientId, senderId, sum+1, chatId);
        // Assert that debt is increased
        Debt debt = debtRepository.getDebtBetweenUsers(senderId, recipientId, chatId);
        assertNotNull(debt);
        assertEquals(-1, debt.getSum());
        assertEquals(senderId, debt.getSenderId());
        assertEquals(recipientId, debt.getRecipientId());
        assertEquals(chatId, debt.getChatId());
    }
}
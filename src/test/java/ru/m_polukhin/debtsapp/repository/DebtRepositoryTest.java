package ru.m_polukhin.debtsapp.repository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.data.jdbc.DataJdbcTest;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.data.domain.PageRequest;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import ru.m_polukhin.debtsapp.models.Debt;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers
@DataJdbcTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
public class DebtRepositoryTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>(
            "postgres:15-alpine"
    );

    @Autowired
    private DebtRepository debtRepository;

    @Autowired
    private UserRepository userRepository;

    @BeforeEach
    public void setUp() {
        debtRepository.deleteAllDebts();
        userRepository.deleteAll();
        userRepository.insertUser(1L, "testUser1");
        userRepository.insertUser(2L, "testUser2");
        userRepository.insertUser(3L, "testUser3");

    }

    @Test
    public void testIncreaseDebt_InsertNewDebt() {
        // Insert a new debt
        debtRepository.increaseDebt(1L, 2L, 100L, 1L);

        // Assert that the debt is inserted correctly
//        Optional<Debt> debt = debtRepository.findById(new Debt.DebtId(1L, 2L, 1L));
//        assertThat(debt).isPresent();
//        assertThat(debt.get().getSum()).isEqualTo(100L);
    }

    @Test
    public void testIncreaseDebt_UpdateExistingDebt() {
        // Insert an existing debt
        debtRepository.increaseDebt(1L, 2L, 100L, 1L);

        // Update the existing debt
        debtRepository.increaseDebt(1L, 2L, 200L, 1L);

        // Assert that the debt is updated correctly
//        Optional<Debt> debt = debtRepository.findById(new Debt.DebtId(1L, 2L, 1L));
//        assertThat(debt).isPresent();
//        assertThat(debt.get().getSum()).isEqualTo(300L); // Sum should be updated to 100 + 200 = 300
    }

    @Test
    public void testIncreaseDebt_ReverseExistingDebt() {
        // Insert an existing debt
        debtRepository.increaseDebt(1L, 2L, 100L, 1L);

        // Update the existing debt
        debtRepository.increaseDebt(2L, 1L, 200L, 1L);

        // Assert that the debt is updated correctly
        List<Debt> debt = debtRepository.findAllDebtsRelated(1L, PageRequest.of(0, 10));
        assertThat(debt).size().isEqualTo(1);
        assertThat(debt.getFirst().getSum()).isEqualTo(-100L); // Sum should be updated to 100 + 200 = 300
    }

    @Test
    public void testGetDebtBetweenUsers_WithChatId_Exists() {
        // Insert a debt between users in a specific chat
        debtRepository.increaseDebt(1L, 2L, 100L, 1L);

        // Retrieve the debt
        Debt debt = debtRepository.getDebtBetweenUsers(1L, 2L, 1L);

        // Assert that the debt is retrieved correctly
        assertThat(debt).isNotNull();
        assertThat(debt.getSum()).isEqualTo(100L);
    }

    @Test
    public void testGetDebtBetweenUsers_WithChatId_Reversed() {
        // Insert a debt between users in a specific chat
        debtRepository.increaseDebt(1L, 2L, 100L, 1L);

        // Retrieve the debt
        Debt debt = debtRepository.getDebtBetweenUsers(2L, 1L, 1L);

        // Assert that the debt is retrieved correctly
        assertThat(debt).isNotNull();
        assertThat(debt.getSum()).isEqualTo(100L);
    }

    @Test
    public void testGetDebtBetweenUsers_WithChatId_NotExists() {
        // Ensure no debt exists between users in a specific chat
        debtRepository.deleteAllByChatId(1L);

        // Retrieve the debt
        Debt debt = debtRepository.getDebtBetweenUsers(1L, 2L, 1L);

        // Assert that no debt is retrieved
        assertThat(debt).isNull();
    }

    @Test
    public void testFindAllDebtsRelated() {
        // Insert debts related to a user
        debtRepository.increaseDebt(1L, 2L, 100L, 1L);

        // Retrieve debts related to the user
        List<Debt> debts = debtRepository.findAllDebtsRelated(1L, PageRequest.of(0, 10));

        // Assert that debts are retrieved correctly
        assertThat(debts).isNotEmpty();
    }

    @Test
    public void testFindAllDebtsRelated_NoDebts() {
        // Ensure no debts related to the user exist
        debtRepository.deleteAllByChatId(1L);

        // Retrieve debts related to the user
        List<Debt> debts = debtRepository.findAllDebtsRelated(1L, PageRequest.of(0, 10));

        // Assert that no debts are retrieved
        assertThat(debts).isEmpty();
    }

    @Test
    public void testFindAllUniqueChatIds() {
        // Insert some debts
        debtRepository.increaseDebt(1L, 2L, 100L, 1L);
        debtRepository.increaseDebt(1L, 3L, 200L, 2L);

        // Retrieve unique chat IDs
        List<Long> chatIds = debtRepository.findAllUniqueChatIds();

        // Assert that unique chat IDs are retrieved correctly
        assertThat(chatIds).containsExactlyInAnyOrder(1L, 2L);
    }

    @Test
    public void testFindByChatId() {
        // Insert some debts associated with a chat ID
        debtRepository.increaseDebt(1L, 2L, 100L, 1L);
        debtRepository.increaseDebt(1L, 3L, 200L, 1L);

        // Retrieve debts by chat ID
        List<Debt> debts = debtRepository.findByChatId(1L, PageRequest.of(0, 10));

        // Assert that debts are retrieved correctly
        assertThat(debts).isNotEmpty();
    }

    @Test
    public void testFindByChatId_NoDebts() {
        // Ensure no debts associated with the chat ID
        debtRepository.deleteAllByChatId(1L);

        // Retrieve debts by chat ID
        List<Debt> debts = debtRepository.findByChatId(1L, PageRequest.of(0, 10));

        // Assert that no debts are retrieved
        assertThat(debts).isEmpty();
    }

    @Test
    public void testDeleteAllByChatId() {
        // Insert some debts associated with a chat ID
        debtRepository.increaseDebt(1L, 2L, 100L, 1L);
        debtRepository.increaseDebt(1L, 3L, 200L, 1L);

        // Delete all debts associated with the chat ID
        debtRepository.deleteAllByChatId(1L);

        // Ensure no debts are associated with the chat ID
        List<Debt> debts = debtRepository.findByChatId(1L, PageRequest.of(0, 10));
        assertThat(debts).isEmpty();
    }
}
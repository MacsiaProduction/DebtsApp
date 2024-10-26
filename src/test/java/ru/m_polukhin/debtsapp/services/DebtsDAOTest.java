package ru.m_polukhin.debtsapp.services;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.TestPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import ru.m_polukhin.debtsapp.dto.DebtInfo;
import ru.m_polukhin.debtsapp.dto.TransactionInfo;
import ru.m_polukhin.debtsapp.exceptions.ParseException;
import ru.m_polukhin.debtsapp.exceptions.UserNotFoundException;
import ru.m_polukhin.debtsapp.repository.DebtRepository;
import ru.m_polukhin.debtsapp.repository.SessionRepository;
import ru.m_polukhin.debtsapp.repository.TransactionRepository;
import ru.m_polukhin.debtsapp.repository.UserRepository;
import ru.m_polukhin.debtsapp.utils.TokenUtils;

import java.util.List;
import java.util.Objects;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertThrows;

@Testcontainers
@SpringBootTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@TestPropertySource(locations = "classpath:./application.properties")
public class DebtsDAOTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>(
            "postgres:16-alpine"
    );

    @Autowired
    private DebtsDAO debtsDAO;

    @Autowired
    private DebtRepository debtRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private SessionRepository sessionRepository;

    @Autowired
    private TokenUtils tokenUtils;

    @BeforeEach
    public void setUp() {
        // Clean up repositories
        debtRepository.deleteAllDebts();
        transactionRepository.deleteAll();
        sessionRepository.deleteAll();
        userRepository.deleteAll();

        // Insert test data
        userRepository.insertUser(1L, "user1");
        userRepository.insertUser(2L, "user2");
        userRepository.insertUser(3L, "user3");
    }

    @Test
    public void testAddTransaction() throws UserNotFoundException, ParseException {
        // Given
        Long chatId = 1L;
        Long senderId = 1L;
        String recipient = "user2";
        Long sum = 100L;
        String comment = "Test transaction";

        // When
        TransactionInfo transactionInfo = debtsDAO.addTransaction(chatId, senderId, recipient, sum, comment);
        DebtInfo debtInfo = debtsDAO.getDebt(chatId, "user1", "user2");

        // Then
        assertThat(transactionInfo).isNotNull();
        assertThat(transactionInfo.sender()).isEqualTo("user1");
        assertThat(transactionInfo.recipient()).isEqualTo("user2");
        assertThat(transactionInfo.sum()).isEqualTo(sum);
        assertThat(transactionInfo.comment()).isEqualTo(comment);
        assertThat(transactionInfo.chatId()).isEqualTo(chatId);

        assertThat(debtInfo).isNotNull();
        assertThat(debtInfo.from()).isEqualTo("user1");
        assertThat(debtInfo.to()).isEqualTo("user2");
        assertThat(debtInfo.sum()).isEqualTo(sum);
        assertThat(debtInfo.chatId()).isEqualTo(chatId);
    }

    @Test
    public void testAddTransactionReverse() throws UserNotFoundException, ParseException {
        // Given
        long chatId = 1L;
        long sum = 100L;
        String comment = "Test transaction";

        // When
        TransactionInfo _transactionInfo1 = debtsDAO.addTransaction(chatId, 1L, "user2", sum, comment);
        TransactionInfo _transactionInfo2 = debtsDAO.addTransaction(chatId, 2L, "user1", 3*sum, comment);
        DebtInfo debtInfo = debtsDAO.getDebt(chatId, "user1", "user2");

        // Then
        assertThat(debtInfo).isNotNull();
        assertThat(debtInfo.from()).isIn("user1", "user2");
        assertThat(debtInfo.to()).isIn("user1", "user2").isNotEqualTo(debtInfo.from());

        assertThat(debtInfo.sum()).isIn(2*sum, -2*sum);
        if (Objects.equals(debtInfo.from(), "user1")) {
            assertThat(debtInfo.sum()).isEqualTo(-2*sum);
        } else if (Objects.equals(debtInfo.from(), "user2")) {
            assertThat(debtInfo.sum()).isEqualTo(2*sum);
        }

        assertThat(debtInfo.chatId()).isEqualTo(chatId);
    }

    @Test
    public void testAddUser() {
        // Given
        Long userId = 3L;
        String username = "user3";

        // When
        debtsDAO.addUser(userId, username);

        // Then
        boolean userExists = userRepository.existsById(userId);
        assertThat(userExists).isTrue();
    }

    @Test
    public void testFindAllTransactionsRelated() throws UserNotFoundException, ParseException {
        // Given
        Long chatId = 1L;
        Long from = 1L;
        String to = "user2";
        String comment = "comment";

        debtsDAO.addTransaction(chatId, from, to, 100L, comment);
        PageRequest pageable = PageRequest.of(0, 10);

        // When
        var transactionsPage = debtsDAO.findAllTransactionsRelated(chatId, from, pageable);

        // Then
        assertThat(transactionsPage).isNotNull();
        assertThat(transactionsPage.getContent()).size().isEqualTo(1);

        var transactionInfo = transactionsPage.getContent().get(0);
        assertThat(transactionInfo.sender()).isEqualTo("user1");
        assertThat(transactionInfo.recipient()).isEqualTo("user2");
        assertThat(transactionInfo.comment()).isEqualTo(comment);
        assertThat(transactionInfo.sum()).isEqualTo(100L);
    }

    @Test
    public void testGetDebt() throws UserNotFoundException {
        // Given
        Long chatId = 1L;
        Long from = 1L;
        Long to = 2L;
        debtRepository.increaseDebt(from, to, 100L, chatId);

        // When
        DebtInfo debtInfo = debtsDAO.getDebt(chatId, "user1", "user2");

        // Then
        assertThat(debtInfo).isNotNull();
        assertThat(debtInfo.from()).isEqualTo("user1");
        assertThat(debtInfo.to()).isEqualTo("user2");
        assertThat(debtInfo.sum()).isEqualTo(100L);
        assertThat(debtInfo.chatId()).isEqualTo(chatId);
    }

    @Test
    public void testFindAllDebtsRelated() throws UserNotFoundException {
        // Given
        Long chatId = 1L;
        Long from = 1L;
        Long to = 2L;
        debtRepository.increaseDebt(from, to, 100L, chatId);
        PageRequest pageable = PageRequest.of(0, 10);

        // When
        Page<DebtInfo> debtsPageFrom = debtsDAO.findAllDebtsRelated(from, pageable);
        Page<DebtInfo> debtsPageTo = debtsDAO.findAllDebtsRelated(to, pageable);

        // Then
        assertThat(debtsPageFrom).isNotNull();
        assertThat(debtsPageFrom.getContent()).size().isEqualTo(1);

        assertThat(debtsPageTo).isNotNull();
        assertThat(debtsPageTo.getContent()).size().isEqualTo(1);

        assertThat(debtsPageFrom.getContent().get(0)).isEqualTo(debtsPageTo.getContent().get(0));
    }

    @Test
    public void testFindAllDebtsRelatedInChat() throws UserNotFoundException {
        // Given
        Long chatId = 1L;
        Long from = 1L;
        Long to = 2L;
        debtRepository.increaseDebt(from, to, 100L, chatId);
        PageRequest pageable = PageRequest.of(0, 10);

        // When
        Page<DebtInfo> debtsPageFrom = debtsDAO.findAllDebtsRelated(chatId, from, pageable);
        Page<DebtInfo> debtsPageTo = debtsDAO.findAllDebtsRelated(chatId, to, pageable);

        // Then
        assertThat(debtsPageFrom).isNotNull();
        assertThat(debtsPageFrom.getContent()).size().isEqualTo(1);

        assertThat(debtsPageTo).isNotNull();
        assertThat(debtsPageTo.getContent()).size().isEqualTo(1);

        assertThat(debtsPageFrom.getContent().get(0)).isEqualTo(debtsPageTo.getContent().get(0));
    }

    @Test
    public void testGetAllChats() {
        // When
        List<Long> chatIds = debtsDAO.getAllChats();

        // Then
        assertThat(chatIds).isNotNull();
        // Additional assertions based on the expected chat IDs
    }

    @Test
    public void testFindUserByNameNotFound() {
        // Given
        String username = "nonExistingUser";

        // When/Then
        assertThrows(UserNotFoundException.class, () -> debtsDAO.findUserByName(username));
    }

// Additional test methods for other functionalities and edge cases in DebtsDAO
}
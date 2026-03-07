package ru.m_polukhin.debtsapp.services;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.Neo4jContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import ru.m_polukhin.debtsapp.dto.DebtInfo;
import ru.m_polukhin.debtsapp.dto.TransactionInfo;
import ru.m_polukhin.debtsapp.exceptions.ParseException;
import ru.m_polukhin.debtsapp.exceptions.UserNotFoundException;
import ru.m_polukhin.debtsapp.models.ActiveSessionToken;
import ru.m_polukhin.debtsapp.models.UserData;
import ru.m_polukhin.debtsapp.repository.SessionRepository;
import ru.m_polukhin.debtsapp.repository.TransactionRepository;
import ru.m_polukhin.debtsapp.repository.UserRepository;
import ru.m_polukhin.debtsapp.utils.TokenUtils;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertThrows;

@Testcontainers
@SpringBootTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
public class DebtsDAOTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15-alpine");

    @Container
    static Neo4jContainer<?> neo4j = new Neo4jContainer<>("neo4j:5-community")
            .withoutAuthentication();

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.neo4j.uri", neo4j::getBoltUrl);
        registry.add("spring.neo4j.authentication.username", () -> "neo4j");
        registry.add("spring.neo4j.authentication.password", () -> "");
    }

    @Autowired private DebtsDAO debtsDAO;
    @Autowired private DebtGraphService debtGraphService;
    @Autowired private UserRepository userRepository;
    @Autowired private TransactionRepository transactionRepository;
    @Autowired private SessionRepository sessionRepository;
    @Autowired private TokenUtils tokenUtils;

    private UserData user1, user2;

    @BeforeEach
    public void setUp() {
        debtGraphService.deleteAll();
        transactionRepository.deleteAll();
        sessionRepository.deleteAll();
        userRepository.deleteAll();

        user1 = userRepository.save(new UserData(null, "user1", 1L, null, null));
        user2 = userRepository.save(new UserData(null, "user2", 2L, null, null));
        userRepository.save(new UserData(null, "user3", 3L, null, null));
    }

    @Test
    public void testAddTransaction() throws UserNotFoundException, ParseException {
        Long chatId = 1L;
        String comment = "Test transaction";
        Long sum = 100L;

        TransactionInfo transactionInfo = debtsDAO.addTransaction(chatId, user1.getId(), "user2", sum, comment);
        DebtInfo debtInfo = debtsDAO.getDebt(chatId, "user1", "user2");

        assertThat(transactionInfo.sender()).isEqualTo("user1");
        assertThat(transactionInfo.recipient()).isEqualTo("user2");
        assertThat(transactionInfo.sum()).isEqualTo(sum);
        assertThat(transactionInfo.comment()).isEqualTo(comment);
        assertThat(transactionInfo.chatId()).isEqualTo(chatId);

        assertThat(debtInfo.from()).isEqualTo("user1");
        assertThat(debtInfo.to()).isEqualTo("user2");
        assertThat(debtInfo.sum()).isEqualTo(sum);
        assertThat(debtInfo.chatId()).isEqualTo(chatId);
    }

    @Test
    public void testAddTransactionReverse() throws UserNotFoundException, ParseException {
        long chatId = 1L;
        long sum = 100L;

        debtsDAO.addTransaction(chatId, user1.getId(), "user2", sum, "");
        debtsDAO.addTransaction(chatId, user2.getId(), "user1", 3 * sum, "");
        DebtInfo debtInfo = debtsDAO.getDebt(chatId, "user1", "user2");

        assertThat(debtInfo.from()).isIn("user1", "user2");
        assertThat(debtInfo.to()).isIn("user1", "user2").isNotEqualTo(debtInfo.from());
        assertThat(debtInfo.sum()).isEqualTo(2 * sum);
    }

    @Test
    public void testAddTelegramUser() {
        UserData saved = debtsDAO.addTelegramUser(99L, "newuser");
        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getTelegramId()).isEqualTo(99L);

        // Idempotent
        UserData saved2 = debtsDAO.addTelegramUser(99L, "newuser");
        assertThat(saved2.getId()).isEqualTo(saved.getId());
    }

    @Test
    public void testFindAllTransactionsRelated() throws UserNotFoundException, ParseException {
        Long chatId = 1L;
        debtsDAO.addTransaction(chatId, user1.getId(), "user2", 100L, "comment");

        var page = debtsDAO.findAllTransactionsRelated(chatId, user1.getId(), PageRequest.of(0, 10));

        assertThat(page.getContent()).hasSize(1);
        assertThat(page.getContent().get(0).sender()).isEqualTo("user1");
        assertThat(page.getContent().get(0).recipient()).isEqualTo("user2");
    }

    @Test
    public void testGetDebt() throws UserNotFoundException {
        Long chatId = 1L;
        debtGraphService.increaseDebt(user1.getId(), user2.getId(), 100L, chatId);

        DebtInfo debtInfo = debtsDAO.getDebt(chatId, "user1", "user2");

        assertThat(debtInfo.from()).isEqualTo("user1");
        assertThat(debtInfo.to()).isEqualTo("user2");
        assertThat(debtInfo.sum()).isEqualTo(100L);
        assertThat(debtInfo.chatId()).isEqualTo(chatId);
    }

    @Test
    public void testFindAllDebtsRelated() throws UserNotFoundException {
        Long chatId = 1L;
        debtGraphService.increaseDebt(user1.getId(), user2.getId(), 100L, chatId);

        Page<DebtInfo> debtsFrom = debtsDAO.findAllDebtsRelated(user1.getId(), PageRequest.of(0, 10));
        Page<DebtInfo> debtsTo = debtsDAO.findAllDebtsRelated(user2.getId(), PageRequest.of(0, 10));

        assertThat(debtsFrom.getContent()).hasSize(1);
        assertThat(debtsTo.getContent()).hasSize(1);
        assertThat(debtsFrom.getContent().get(0)).isEqualTo(debtsTo.getContent().get(0));
    }

    @Test
    public void testFindAllDebtsRelatedInChat() throws UserNotFoundException {
        Long chatId = 1L;
        debtGraphService.increaseDebt(user1.getId(), user2.getId(), 100L, chatId);

        Page<DebtInfo> debtsFrom = debtsDAO.findAllDebtsRelated(chatId, user1.getId(), PageRequest.of(0, 10));
        Page<DebtInfo> debtsTo = debtsDAO.findAllDebtsRelated(chatId, user2.getId(), PageRequest.of(0, 10));

        assertThat(debtsFrom.getContent()).hasSize(1);
        assertThat(debtsTo.getContent()).hasSize(1);
        assertThat(debtsFrom.getContent().get(0)).isEqualTo(debtsTo.getContent().get(0));
    }

    @Test
    public void testGetAllChats() {
        List<Long> chatIds = debtsDAO.getAllChats();
        assertThat(chatIds).isNotNull();
    }

    @Test
    public void testActiveSession() throws UserNotFoundException {
        debtsDAO.addActiveSession(tokenUtils.generateSessionToken(user1.getId(), "hash"));

        ActiveSessionToken token = debtsDAO.getUsersSession(user1.getId());
        assertThat(token.userId()).isEqualTo(user1.getId());
        assertThat(token.hash()).isEqualTo("hash");
        assertThat(token.expirationDate()).isInTheFuture();
    }

    @Test
    public void testFindUserByNameNotFound() {
        assertThrows(UserNotFoundException.class, () -> debtsDAO.findUserByName("nonExistingUser"));
    }
}

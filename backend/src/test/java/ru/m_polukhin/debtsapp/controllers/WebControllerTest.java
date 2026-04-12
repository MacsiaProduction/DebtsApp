package ru.m_polukhin.debtsapp.controllers;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;
import org.testcontainers.containers.Neo4jContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import ru.m_polukhin.debtsapp.dto.DebtInfo;
import ru.m_polukhin.debtsapp.dto.TransactionInfo;
import ru.m_polukhin.debtsapp.exceptions.ParseException;
import ru.m_polukhin.debtsapp.exceptions.UserNotFoundException;
import ru.m_polukhin.debtsapp.models.UserData;
import ru.m_polukhin.debtsapp.repository.SessionRepository;
import ru.m_polukhin.debtsapp.repository.TransactionRepository;
import ru.m_polukhin.debtsapp.repository.UserRepository;
import ru.m_polukhin.debtsapp.services.DebtsDAO;
import ru.m_polukhin.debtsapp.services.DebtGraphService;
import ru.m_polukhin.debtsapp.utils.CustomPageImpl;

import java.security.Principal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc(addFilters = false)
@Testcontainers
@SuppressWarnings({"resource", "null"})
public class WebControllerTest {

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
    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;

    private UserData user1, user2, user3;

    @BeforeEach
    public void setUp() throws UserNotFoundException, ParseException {
        debtGraphService.deleteAll();
        transactionRepository.deleteAll();
        sessionRepository.deleteAll();
        userRepository.deleteAll();

        user1 = userRepository.save(new UserData(null, "user1", 1L, null, null));
        user2 = userRepository.save(new UserData(null, "user2", 2L, null, null));
        user3 = userRepository.save(new UserData(null, "user3", 3L, null, null));

        debtsDAO.addTransaction(1L, user1.getId(), "user2", 1L, "message10");
        debtsDAO.addTransaction(1L, user1.getId(), "user3", 10L, "message11");
        debtsDAO.addTransaction(1L, user2.getId(), "user3", 100L, "message12");
        debtsDAO.addTransaction(2L, user3.getId(), "user1", 2L, "message20");
        debtsDAO.addTransaction(2L, user3.getId(), "user2", 20L, "message21");
    }

    private Principal principalOf(UserData user) {
        Principal p = Mockito.mock(Principal.class);
        Mockito.when(p.getName()).thenReturn(String.valueOf(user.getId()));
        return p;
    }

    @Test
    @Order(1)
    public void setUpTests() {}

    @Test
    public void testFindAllTransactionsRelated() throws Exception {
        var content = mockMvc.perform(MockMvcRequestBuilders.get("/transactions")
                        .principal(principalOf(user1)))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andReturn().getResponse().getContentAsString();

        Page<TransactionInfo> response = objectMapper.readValue(content, new TypeReference<CustomPageImpl<TransactionInfo>>() {});

        List<TransactionInfo> expected = List.of(
                new TransactionInfo(null, "user1", "user2", 1L, 1L, "message10"),
                new TransactionInfo(null, "user1", "user3", 10L, 1L, "message11"),
                new TransactionInfo(null, "user3", "user1", 2L, 2L, "message20")
        );
        assertThat(response.getContent().size()).isEqualTo(expected.size());
        assertThat(response.getContent().stream().map(TransactionInfo::comment).toList())
                .containsAll(expected.stream().map(TransactionInfo::comment).toList());
    }

    @Test
    public void testCreateTransaction() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.post("/new")
                        .principal(principalOf(user1))
                        .param("chatId", "1")
                        .param("toName", "user2")
                        .param("sum", "500")
                        .param("comment", "created"))
                .andExpect(status().isCreated());

        var debt = debtsDAO.getDebt(1L, "user1", "user2");
        assertThat(debt.sum()).isEqualTo(501L);
    }

    @Test
    public void testCreateTransactionUsesDefaultWebContext() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.post("/new")
                        .principal(principalOf(user1))
                        .param("toName", "user2")
                        .param("sum", "5")
                        .param("comment", "web-default"))
                .andExpect(status().isCreated());

        var debt = debtsDAO.getDebt(0L, "user1", "user2");
        assertThat(debt.sum()).isEqualTo(5L);
    }

    @Test
    public void testGetDebt() throws Exception {
        var content = mockMvc.perform(MockMvcRequestBuilders.get("/debts/between")
                        .principal(principalOf(user1))
                        .param("fromName", "user1")
                        .param("toName", "user2")
                        .param("chatId", "1"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        DebtInfo response = objectMapper.readValue(content, DebtInfo.class);

        assertThat(response.sum()).isEqualTo(1L);
        assertThat(response.chatId()).isEqualTo(1);
        assertThat(response.from()).isEqualTo("user1");
        assertThat(response.to()).isEqualTo("user2");
    }

    @Test
    public void testFindAllDebtsRelated() throws Exception {
        var content = mockMvc.perform(MockMvcRequestBuilders.get("/debts")
                        .principal(principalOf(user1))
                        .param("size", "10")
                        .characterEncoding("utf-8"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        Page<DebtInfo> response = objectMapper.readValue(content, new TypeReference<CustomPageImpl<DebtInfo>>() {});
        var expected = debtsDAO.findAllDebtsRelated(user1.getId(), PageRequest.of(0, 10));

        assertThat(response.getContent().containsAll(expected.getContent())).isTrue();
    }

    @Test
    public void testFindAllTransactionsRelatedByChatId() throws Exception {
        var content = mockMvc.perform(MockMvcRequestBuilders.get("/transactions/chat")
                        .principal(principalOf(user1))
                        .param("chatId", "1"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        Page<TransactionInfo> response = objectMapper.readValue(content, new TypeReference<CustomPageImpl<TransactionInfo>>() {});
        var expected = debtsDAO.findAllTransactionsRelated(1L, user1.getId(), PageRequest.of(0, 10));

        assertThat(response.getContent().containsAll(expected.getContent())).isTrue();
    }

    @Test
    public void testDeleteLastTransaction() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.post("/new")
                        .principal(principalOf(user1))
                        .param("chatId", "1")
                        .param("toName", "user2")
                        .param("sum", "500")
                        .param("comment", "latest"))
                .andExpect(status().isCreated());

        mockMvc.perform(MockMvcRequestBuilders.delete("/transactions/last")
                        .principal(principalOf(user1)))
                .andExpect(status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath("$.comment").value("latest"));

        var debt = debtsDAO.getDebt(1L, "user1", "user2");
        assertThat(debt.sum()).isEqualTo(1L);
    }

    @Test
    public void testUpdateTransactionComment() throws Exception {
        var transaction = debtsDAO.addTransaction(1L, user1.getId(), "user2", 11L, "old-comment");

        mockMvc.perform(MockMvcRequestBuilders.patch("/transactions/{transactionId}/comment", transaction.id())
                        .principal(principalOf(user1))
                        .param("comment", "new-comment"))
                .andExpect(status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath("$.id").value(transaction.id()))
                .andExpect(MockMvcResultMatchers.jsonPath("$.comment").value("new-comment"));

        var content = mockMvc.perform(MockMvcRequestBuilders.get("/transactions/chat")
                        .principal(principalOf(user1))
                        .param("chatId", "1"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        Page<TransactionInfo> response = objectMapper.readValue(content, new TypeReference<CustomPageImpl<TransactionInfo>>() {});
        assertThat(response.getContent().stream().anyMatch(tx -> "new-comment".equals(tx.comment()))).isTrue();
    }
}

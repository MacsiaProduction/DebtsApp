package ru.m_polukhin.debtsapp.controllers;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;
import ru.m_polukhin.debtsapp.dto.DebtInfo;
import ru.m_polukhin.debtsapp.dto.TransactionInfo;
import ru.m_polukhin.debtsapp.exceptions.ParseException;
import ru.m_polukhin.debtsapp.exceptions.UserNotFoundException;
import ru.m_polukhin.debtsapp.repository.DebtRepository;
import ru.m_polukhin.debtsapp.repository.TransactionRepository;
import ru.m_polukhin.debtsapp.repository.UserRepository;
import ru.m_polukhin.debtsapp.services.DebtsDAO;
import ru.m_polukhin.debtsapp.utils.CustomPageImpl;

import java.security.Principal;
import java.util.List;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc(addFilters = false)
public class WebControllerTest {

    @Autowired
    private DebtsDAO debtsDAO;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private DebtRepository debtRepository;

    @Autowired
    private MockMvc mockMvc;

    private final List<TransactionInfo> transactionInfos1;

    @Autowired
    private ObjectMapper objectMapper;

    public WebControllerTest() {
        List<TransactionInfo> transactionInfos = List.of(
                new TransactionInfo("user1", "user2", 1L, 1L, "message10"),
                new TransactionInfo("user1", "user3", 10L, 1L, "message11"),
                new TransactionInfo("user2", "user3", 100L, 1L, "message12"),

                new TransactionInfo("user3", "user1", 2L, 2L, "message20"),
                new TransactionInfo("user3", "user2", 20L, 2L, "message21")
        );
        transactionInfos1 = transactionInfos.stream()
                .filter(transactionInfo -> transactionInfo.sender().equals("user1") || transactionInfo.recipient().equals("user1"))
                .toList();
    }

    @BeforeEach
    public void setUp() throws UserNotFoundException, ParseException {
        transactionRepository.deleteAll();
        debtRepository.deleteAllDebts();
        userRepository.deleteAll();
        //test info
        debtsDAO.addUser(1L, "user1");
        debtsDAO.addUser(2L, "user2");
        debtsDAO.addUser(3L, "user3");

        debtsDAO.addTransaction(1L, 1L, "user2", 1L, "message10");
        debtsDAO.addTransaction(1L, 1L, "user3", 10L, "message11");
        debtsDAO.addTransaction(1L, 2L, "user3", 100L, "message12");

        debtsDAO.addTransaction(2L, 3L, "user1", 2L, "message20");
        debtsDAO.addTransaction(2L, 3L, "user2", 20L, "message21");
    }

    @Test
    public void setUpTests() {}

    @Test
    public void testFindAllTransactionsRelated() throws Exception {

        Principal principal = Mockito.mock(Principal.class);
        Mockito.when(principal.getName()).thenReturn("1");

        var content = mockMvc.perform(MockMvcRequestBuilders.get("/transactions")
                        .principal(principal)
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andReturn().getResponse().getContentAsString();

        Page<TransactionInfo> response = objectMapper.readValue(content, new TypeReference<CustomPageImpl<TransactionInfo>>() {});

        assertThat(response.getContent().size()).isEqualTo(transactionInfos1.size());
        assertThat(response.getContent().containsAll(transactionInfos1)).isTrue();
    }

    @Test
    public void testCreateTransaction() throws Exception {
        Principal principal = Mockito.mock(Principal.class);
        Mockito.when(principal.getName()).thenReturn("1");

        var transaction = new TransactionInfo("user1", "user2", 500L, 1L, "created");

        mockMvc.perform(MockMvcRequestBuilders.post("/new")
                        .principal(principal)
                        .param("chatId", "1")
                        .param("toName", "user2")
                        .param("sum", "500")
                        .param("comment", "created"))
                .andExpect(status().isCreated());

        Page<TransactionInfo> allTransactionsRelated = debtsDAO.findAllTransactionsRelated(1L, PageRequest.of(0, 10));
        assertThat(allTransactionsRelated.getContent().contains(transaction)).isTrue();

        var debt = debtsDAO.getDebt(1L, "user1", "user2");
        assertThat(debt.sum()).isEqualTo(501L);
    }

    @Test
    public void testGetDebt() throws Exception {
        Principal principal = Mockito.mock(Principal.class);
        Mockito.when(principal.getName()).thenReturn("1");

        var content = mockMvc.perform(MockMvcRequestBuilders.get("/debts/between")
                        .principal(principal)
                        .param("fromName", "user1")
                        .param("toName", "user2")
                        .param("chatId", "1"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        DebtInfo response = objectMapper.readValue(content, DebtInfo.class);

        assertThat(response.sum()).isIn(1L, -1L);
        assertThat(response.chatId()).isEqualTo(1);
        assertThat(response.from()).isIn("user1","user2");
        assertThat(response.to()).isIn("user1","user2").isNotEqualTo(response.from());
    }

    @Test
    public void testFindAllDebtsRelated() throws Exception {
        Principal principal = Mockito.mock(Principal.class);
        Mockito.when(principal.getName()).thenReturn("1");

        var content = mockMvc.perform(MockMvcRequestBuilders.get("/debts")
                        .principal(principal)
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        Page<DebtInfo> response = objectMapper.readValue(content, new TypeReference<CustomPageImpl<DebtInfo>>() {});

        var expected = debtsDAO.findAllDebtsRelated(1L, PageRequest.of(0,10));
        assertThat(response.getSize()).isEqualTo(expected.getSize());
        assertThat(response.getContent().containsAll(expected.getContent())).isTrue();
    }

    @Test
    public void testFindAllTransactionsRelatedByChatId() throws Exception {
        Principal principal = Mockito.mock(Principal.class);
        Mockito.when(principal.getName()).thenReturn("1");

        var content = mockMvc.perform(MockMvcRequestBuilders.get("/transactions/chat")
                        .principal(principal)
                        .param("chatId", "1")
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        Page<TransactionInfo> response = objectMapper.readValue(content, new TypeReference<CustomPageImpl<TransactionInfo>>() {});

        var expected = debtsDAO.findAllTransactionsRelated(1L, 1L, PageRequest.of(0,10));
        assertThat(response.getSize()).isEqualTo(expected.getSize());
        assertThat(response.getContent().containsAll(expected.getContent())).isTrue();
    }
}
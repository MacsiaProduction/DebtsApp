package ru.m_polukhin.debtsapp.controllers;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import ru.m_polukhin.debtsapp.dto.DebtInfo;
import ru.m_polukhin.debtsapp.dto.TransactionInfo;
import ru.m_polukhin.debtsapp.exceptions.ParseException;
import ru.m_polukhin.debtsapp.exceptions.UserNotFoundException;
import ru.m_polukhin.debtsapp.services.DebtsDAO;
import ru.m_polukhin.debtsapp.utils.CustomPageImpl;

import java.security.Principal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class WebControllerTest {

    private static final Principal USER_PRINCIPAL = () -> "1";

    @Mock
    private DebtsDAO debtsDAO;

    @InjectMocks
    private WebController webController;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(webController).build();
    }

    @Test
    void testFindAllTransactionsRelated() throws Exception {
        when(debtsDAO.findAllTransactionsRelated(eq(1L), any()))
                .thenReturn(new CustomPageImpl<>(List.of(
                        new TransactionInfo(10L, "user1", "user2", 25L, 1L, "first")
                )));

        var response = webController.findAllTransactionsRelated(USER_PRINCIPAL, 0, 10);

        assertThat(response.getContent()).hasSize(1);
        assertThat(response.getContent().getFirst().id()).isEqualTo(10L);
        assertThat(response.getContent().getFirst().comment()).isEqualTo("first");
    }

    @Test
    void testCreateTransactionUsesDefaultWebContext() throws Exception {
        mockMvc.perform(post("/new")
                        .principal(USER_PRINCIPAL)
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param("toName", "user2")
                        .param("sum", "5")
                        .param("comment", "web-default"))
                .andExpect(status().isCreated())
                .andExpect(content().string("Transaction created successfully"));

        verify(debtsDAO).addTransaction(0L, 1L, "user2", 5L, "web-default");
    }

    @Test
    void testCreateTransactionValidationFailureReturnsBadRequest() throws Exception {
        doThrow(new UserNotFoundException("missing"))
                .when(debtsDAO).addTransaction(1L, 1L, "missing", 5L, "bad");

        mockMvc.perform(post("/new")
                        .principal(USER_PRINCIPAL)
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param("chatId", "1")
                        .param("toName", "missing")
                        .param("sum", "5")
                        .param("comment", "bad"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void testCreateTransactionParseFailureReturnsBadRequest() throws Exception {
        doThrow(new ParseException("bad"))
                .when(debtsDAO).addTransaction(1L, 1L, "user2", 5L, "bad");

        mockMvc.perform(post("/new")
                        .principal(USER_PRINCIPAL)
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param("chatId", "1")
                        .param("toName", "user2")
                        .param("sum", "5")
                        .param("comment", "bad"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void testGetDebt() throws Exception {
        when(debtsDAO.getIdByName("user1")).thenReturn(1L);
        when(debtsDAO.getIdByName("user2")).thenReturn(2L);
        when(debtsDAO.getDebt(1L, "user1", "user2"))
                .thenReturn(new DebtInfo("user1", "user2", 100L, 1L));

        mockMvc.perform(get("/debts/between")
                        .principal(USER_PRINCIPAL)
                        .param("chatId", "1")
                        .param("fromName", "user1")
                        .param("toName", "user2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sum").value(100))
                .andExpect(jsonPath("$.from").value("user1"))
                .andExpect(jsonPath("$.to").value("user2"));
    }

    @Test
    void testFindAllDebtsRelated() throws Exception {
        when(debtsDAO.findAllDebtsRelated(eq(1L), any()))
                .thenReturn(new CustomPageImpl<>(List.of(
                        new DebtInfo("user1", "user2", 100L, 1L)
                )));

        var response = webController.findAllDebtsRelated(USER_PRINCIPAL, 0, 10);

        assertThat(response.getContent()).hasSize(1);
        assertThat(response.getContent().getFirst().sum()).isEqualTo(100L);
    }

    @Test
    void testFindAllTransactionsRelatedByChatId() throws Exception {
        when(debtsDAO.findAllTransactionsRelated(eq(1L), eq(1L), any()))
                .thenReturn(new CustomPageImpl<>(List.of(
                        new TransactionInfo(11L, "user1", "user2", 99L, 1L, "chat")
                )));

        var response = webController.findAllTransactionsRelated(USER_PRINCIPAL, 1L, 0, 10);

        assertThat(response.getContent()).hasSize(1);
        assertThat(response.getContent().getFirst().comment()).isEqualTo("chat");
    }

    @Test
    void testDeleteTransaction() throws Exception {
        when(debtsDAO.deleteTransaction(1L, 25L))
                .thenReturn(new TransactionInfo(25L, "user1", "user2", 50L, 1L, "latest"));

        mockMvc.perform(delete("/transactions/25").principal(USER_PRINCIPAL))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.comment").value("latest"));
    }

    @Test
    void testUpdateTransactionComment() throws Exception {
        when(debtsDAO.updateTransactionComment(1L, 25L, "new-comment"))
                .thenReturn(new TransactionInfo(25L, "user1", "user2", 50L, 1L, "new-comment"));

        mockMvc.perform(post("/transactions/25/comment")
                        .principal(USER_PRINCIPAL)
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param("comment", "new-comment"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.comment").value("new-comment"));
    }

    @Test
    void testFindAllTransactionsFromTo() throws Exception {
        when(debtsDAO.getIdByName("user1")).thenReturn(1L);
        when(debtsDAO.getIdByName("user2")).thenReturn(2L);
        when(debtsDAO.findAllTransactionsFromTo("user1", "user2", PageRequest.of(0, 10)))
                .thenReturn(new CustomPageImpl<>(List.of(
                        new TransactionInfo(12L, "user1", "user2", 10L, 0L, "between")
                )));

        var response = webController.findAllTransactionsFromTo(
                USER_PRINCIPAL, "user1", "user2", 0, 10);

        assertThat(response.getContent()).hasSize(1);
        assertThat(response.getContent().getFirst().comment()).isEqualTo("between");
    }

    @Test
    void testGetDebtRejectsUnrelatedPrincipal() throws Exception {
        when(debtsDAO.getIdByName("other1")).thenReturn(2L);
        when(debtsDAO.getIdByName("other2")).thenReturn(3L);

        mockMvc.perform(get("/debts/between")
                        .principal(USER_PRINCIPAL)
                        .param("chatId", "1")
                        .param("fromName", "other1")
                        .param("toName", "other2"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void testDeleteTransactionNotFound() throws Exception {
        when(debtsDAO.deleteTransaction(1L, 99L))
                .thenThrow(new IllegalArgumentException("Transaction not found"));

        mockMvc.perform(delete("/transactions/99").principal(USER_PRINCIPAL))
                .andExpect(status().isNotFound());
    }

    @Test
    void testUpdateTransactionCommentForbidden() throws Exception {
        doThrow(new SecurityException("Only sender can edit"))
                .when(debtsDAO).updateTransactionComment(1L, 25L, "hack");

        mockMvc.perform(post("/transactions/25/comment")
                        .principal(USER_PRINCIPAL)
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param("comment", "hack"))
                .andExpect(status().isForbidden());
    }

    @Test
    void testFindAllTransactionsRelatedReturnsBadRequestWhenUserMissing() throws Exception {
        when(debtsDAO.findAllTransactionsRelated(eq(1L), any()))
                .thenThrow(new UserNotFoundException("missing"));

        mockMvc.perform(get("/transactions").principal(USER_PRINCIPAL))
                .andExpect(status().isBadRequest());
    }
}

package ru.m_polukhin.debtsapp.controllers;

import jakarta.validation.constraints.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import ru.m_polukhin.debtsapp.dto.DebtInfo;
import ru.m_polukhin.debtsapp.dto.TransactionInfo;
import ru.m_polukhin.debtsapp.exceptions.ParseException;
import ru.m_polukhin.debtsapp.exceptions.UserNotFoundException;
import ru.m_polukhin.debtsapp.services.DebtsDAO;

import java.security.Principal;

@RestController
@RequiredArgsConstructor
public class WebController {
    private final DebtsDAO dao;

    @GetMapping("transactions")
    public Page<TransactionInfo> findAllTransactionsRelated(
            @NotNull Principal principal,
            @Min(0) @RequestParam(defaultValue = "0") int page,
            @Positive @Max(100) @RequestParam(defaultValue = "10") int size) {
        try {
            Long userId = Long.valueOf(principal.getName());
            return dao.findAllTransactionsRelated(userId, PageRequest.of(page, size));
        } catch (UserNotFoundException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST);
        }
    }

    @GetMapping("transactions/chat")
    public Page<TransactionInfo> findAllTransactionsRelated(
            @NotNull Principal principal,
            @Positive @RequestParam Long chatId,
            @Min(0) @RequestParam(defaultValue = "0") int page,
            @Positive @Max(100) @RequestParam(defaultValue = "10") int size) {
        try {
            Long userId = Long.valueOf(principal.getName());
            return dao.findAllTransactionsRelated(chatId, userId, PageRequest.of(page, size));
        } catch (UserNotFoundException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST);
        }
    }

    @GetMapping("transactions/between")
    public Page<TransactionInfo> findAllTransactionsFromTo(
            @NotNull Principal principal,
            @Size(max = 50) @RequestParam String sender,
            @Size(max = 50) @RequestParam String recipient,
            @Min(0) @RequestParam(defaultValue = "0") int page,
            @Positive @Max(100) @RequestParam(defaultValue = "10") int size) {
        validateTwo(principal, sender, recipient);
        try {
            return dao.findAllTransactionsFromTo(sender, recipient, PageRequest.of(page, size));
        } catch (UserNotFoundException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST);
        }
    }

    @GetMapping("transactions/between/chat")
    public Page<TransactionInfo> findAllTransactionsFromTo(
            @NotNull Principal principal,
            @Positive @RequestParam Long chatId,
            @Size(max = 50) @RequestParam String sender,
            @Size(max = 50) @RequestParam String recipient,
            @Min(0) @RequestParam(defaultValue = "0") int page,
            @Positive @Max(100) @RequestParam(defaultValue = "10") int size) {
        validateTwo(principal, sender, recipient);
        try {
            return dao.findAllTransactionsFromTo(chatId, sender, recipient, PageRequest.of(page, size));
        } catch (UserNotFoundException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST);
        }
    }

    @PostMapping("new")
    public ResponseEntity<String> createTransaction(@NotNull Principal principal,
                                                    @PositiveOrZero @RequestParam(defaultValue = "0") Long chatId,
                                                    @Size(max = 50) @NotBlank @RequestParam String toName,
                                                    @Positive @RequestParam Long sum,
                                                    @Size(max = 50) @RequestParam String comment) {
        try {
            dao.addTransaction(chatId, Long.valueOf(principal.getName()), toName, sum, comment);
            return new ResponseEntity<>("Transaction created successfully", HttpStatus.CREATED);
        } catch (UserNotFoundException | ParseException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST);
        }
    }

    @GetMapping("debts/between")
    public DebtInfo getDebt(
            @NotNull Principal principal,
            @Positive @RequestParam Long chatId,
            @Size(max = 50) @RequestParam String fromName,
            @Size(max = 50) @RequestParam String toName) {
        validateTwo(principal, fromName, toName);
        try {
            return dao.getDebt(chatId, fromName, toName);
        } catch (UserNotFoundException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST);
        }
    }

    @GetMapping("debts")
    public Page<DebtInfo> findAllDebtsRelated(
            @NotNull Principal principal,
            @Min(0) @RequestParam(defaultValue = "0") int page,
            @Positive @Max(100) @RequestParam(defaultValue = "10") int size) {
        try {
            return dao.findAllDebtsRelated(Long.valueOf(principal.getName()), PageRequest.of(page, size));
        } catch (UserNotFoundException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST);
        }
    }

    private void validateTwo(Principal principal, String fromName, String toName) {
        try {
            long name = Long.parseLong(principal.getName());
            var user1 = dao.getIdByName(fromName);
            var user2 = dao.getIdByName(toName);
            if ((name != user1) && (name != user2)) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST);
            }
        } catch (UserNotFoundException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST);
        }
    }
}

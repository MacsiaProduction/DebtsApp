package ru.m_polukhin.debtsapp.controllers;

import io.swagger.annotations.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import ru.m_polukhin.debtsapp.dto.DebtInfo;
import ru.m_polukhin.debtsapp.dto.TransactionInfo;
import ru.m_polukhin.debtsapp.services.DebtsDAO;
import ru.m_polukhin.debtsapp.exceptions.ParseException;
import ru.m_polukhin.debtsapp.exceptions.UserNotFoundException;

import java.security.Principal;

@RestController
@RequiredArgsConstructor
public class WebController {
    //todo now with comment and chatId
    private final DebtsDAO dao;

    @Operation(summary = "Returns page of all transactions related to {user}")
    @GetMapping("transactions")
    @ApiResponses(value = {
            @ApiResponse(code = 201, message = "Transaction created successfully"),
            @ApiResponse(code = 400, message = "Bad request"),
            @ApiResponse(code = 401, message = "Only authorized users allowed")
    })
    public Page<TransactionInfo> findAllTransactionsRelated(
            Principal principal,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        try {
            Long userId = Long.valueOf(principal.getName());
            //todo chatId
            return dao.findAllTransactionsRelated(0L, userId, PageRequest.of(page, size));
        } catch (UserNotFoundException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST);
        }
    }

    @Operation(summary = "Returns page of all transactions from sender to recipient")
    @GetMapping("transactions/between")
    @ApiResponses(value = {
            @ApiResponse(code = 201, message = "Transaction created successfully"),
            @ApiResponse(code = 400, message = "Bad request"),
            @ApiResponse(code = 401, message = "Only authorized users allowed")
    })
    public Page<TransactionInfo> findAllTransactionsFromTo(
            Principal principal,
            @Parameter(description = "Sender") @RequestParam String sender,
            @Parameter(description = "Recipient") @RequestParam String recipient,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        validateTwo(principal, sender, recipient);
        try {
            //todo chatId
            return dao.findAllTransactionsFromTo(0L, sender, recipient, PageRequest.of(page, size));
        } catch (UserNotFoundException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST);
        }
    }

    @Operation(summary = "Create a new transaction")
    @PostMapping("new")
    @ApiResponses(value = {
            @ApiResponse(code = 201, message = "Transaction created successfully"),
            @ApiResponse(code = 400, message = "Bad request"),
            @ApiResponse(code = 401, message = "Only authorized users allowed")
    })
    public ResponseEntity<String> createTransaction(Principal principal,
                                                    @RequestParam String toName,
                                                    @RequestParam Long sum) {
        try {
            //todo chatId, comment
            var transaction = dao.addTransaction(0L, Long.valueOf(principal.getName()), toName, sum, "");
            return new ResponseEntity<>("Transaction created successfully", HttpStatus.CREATED);
        } catch (UserNotFoundException | ParseException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST);
        }
    }

    @Operation(summary = "Returns debt between {fromName} and {toName}")
    @GetMapping("debts/between")
    @ApiResponses(value = {
            @ApiResponse(code = 201, message = "Transaction created successfully"),
            @ApiResponse(code = 400, message = "Bad request"),
            @ApiResponse(code = 401, message = "Only authorized users allowed")
    })
    public DebtInfo getDebt(
            Principal principal,
            @Parameter(description = "Creditor's name") @RequestParam String fromName,
            @Parameter(description = "Debtor's name") @RequestParam String toName) {
        validateTwo(principal, fromName, toName);
        try {
            //todo chatId
            return dao.getDebt(0L, fromName, toName);
        } catch (UserNotFoundException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST);
        }
    }

    @Operation(summary = "Returns all debts related to {name}")
    @GetMapping("debts")
    @ApiResponses(value = {
            @ApiResponse(code = 201, message = "Transaction created successfully"),
            @ApiResponse(code = 400, message = "Bad request"),
            @ApiResponse(code = 401, message = "Only authorized users allowed")
    })
    public Page<DebtInfo> findAllDebtsRelated(
            Principal principal,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        try {
            return dao.findAllDebtsRelated(Long.valueOf(principal.getName()), PageRequest.of(page, size));
        } catch (UserNotFoundException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST);
        }
    }

    private void validateTwo(Principal principal, @RequestParam @Parameter(description = "Creditor's name") String fromName, @RequestParam @Parameter(description = "Debtor's name") String toName) {
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
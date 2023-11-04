package ru.m_polukhin.debtsapp.controllers;

import io.swagger.annotations.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import lombok.RequiredArgsConstructor;
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
import java.util.List;

@RestController
@RequiredArgsConstructor
public class WebController {
    private final DebtsDAO dao;

    //todo Pageable maybe PagedModel
    @Operation(summary = "Returns list of all transactions related to {user}")
    @GetMapping("transactions")
    @ApiResponses(value = {
            @ApiResponse(code = 201, message = "Transaction created successfully"),
            @ApiResponse(code = 400, message = "Bad request"),
            @ApiResponse(code = 401, message = "Only authorized users allowed")
    })
    public List<TransactionInfo> findAllTransactionsRelated(Principal principal) {
        try {
            return dao.findAllTransactionsRelated(Long.valueOf(principal.getName()));
        } catch (UserNotFoundException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST);
        }
    }

    @Operation(summary = "Returns list of all transactions from sender to recipient")
    @GetMapping("transactions/between")
    @ApiResponses(value = {
            @ApiResponse(code = 201, message = "Transaction created successfully"),
            @ApiResponse(code = 400, message = "Bad request"),
            @ApiResponse(code = 401, message = "Only authorized users allowed")
    })
    public List<TransactionInfo> findAllTransactionsFromTo(
            Principal principal,
            @Parameter(description = "Sender") @RequestParam String sender,
            @Parameter(description = "Recipient") @RequestParam String recipient) {
        validateTwo(principal, sender, recipient);
        try {
            return dao.findAllTransactionsFromTo(sender, recipient);
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
    public ResponseEntity<String> createTransaction(Principal principal, @RequestBody TransactionInfo dto) {
        validateOne(principal, dto.sender());
        try {
            dao.addTransaction(dto.sender(), dto.recipient(), dto.sum());
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
            return dao.getDebt(fromName, toName);
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
    public List<DebtInfo> findAllDebtsRelated(
            Principal principal,
            @Parameter(description = "User name") @RequestParam String name) {
        validateOne(principal, name);
        try {
            return dao.findAllDebtsRelated(name);
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

    private void validateOne(Principal principal, @RequestParam @Parameter(description = "Creditor's name") String fromName) {
        try {
            long name = Long.parseLong(principal.getName());
            var user1 = dao.getIdByName(fromName);
            if (name != user1) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST);
            }
        } catch (UserNotFoundException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST);
        }
    }

}
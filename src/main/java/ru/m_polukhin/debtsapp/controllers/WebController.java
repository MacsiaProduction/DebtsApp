package ru.m_polukhin.debtsapp.controllers;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import ru.m_polukhin.debtsapp.exceptions.PasswordsNotMatch;
import ru.m_polukhin.debtsapp.models.ChangePasswordDto;
import ru.m_polukhin.debtsapp.models.DebtInfo;
import ru.m_polukhin.debtsapp.models.LogInDTO;
import ru.m_polukhin.debtsapp.models.TransactionInfo;
import ru.m_polukhin.debtsapp.services.DebtsDAO;
import ru.m_polukhin.debtsapp.exceptions.ParseException;
import ru.m_polukhin.debtsapp.exceptions.UserNotFoundException;
import ru.m_polukhin.debtsapp.services.SecurityService;

import java.util.List;

@RestController
@Api("Controller to work with Debt Calculation Network")
public class WebController {
    private final DebtsDAO dao;
    private final SecurityService securityService;

    @Autowired
    public WebController(DebtsDAO dao, SecurityService securityService) {
        this.dao = dao;
        this.securityService = securityService;
    }

    @ApiOperation("Returns list of all transactions")
    @GetMapping("transactions")
    public List<TransactionInfo> listAll() {
        return dao.findAllTransactions();
    }

    @ApiOperation("Returns list of all transactions related to {user}")
    @GetMapping("transactions/related")
    public List<TransactionInfo> findAllTransactionsRelated(@ApiParam("name") @RequestParam String name) {
        try {
            return dao.findAllTransactionsRelated(name);
        } catch (UserNotFoundException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST);
        }
    }

    @ApiOperation("Returns list of all transactions from {sender} to {recipient}")
    @GetMapping("transactions/between")
    public List<TransactionInfo> findAllTransactionsFromTo(@ApiParam("sender") @RequestParam String sender,
                                                @ApiParam("recipient") @RequestParam String recipient) {
        try {
            return dao.findAllTransactionsFromTo(sender, recipient);
        } catch (UserNotFoundException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST);
        }
    }

    @ApiOperation("Adds new transaction")
    @PostMapping("new")
    public void addTransaction(@ApiParam("transaction") @RequestBody TransactionInfo dto) {
        try {
            dao.addTransaction(dto.sender(), dto.recipient(), dto.sum());
        } catch (UserNotFoundException | ParseException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST);
        }
    }

    @ApiOperation("Returns debt between {fromName} and {toName}")
    @GetMapping("debts/between")
    public DebtInfo getDebt(@ApiParam("from") @RequestParam String fromName,
                            @ApiParam("to") @RequestParam String toName) {
        try {
            return dao.getDebt(fromName, toName);
        } catch (UserNotFoundException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST);
        }
    }

    @ApiOperation("Returns all debts related to {name}")
    @GetMapping("debts/related")
    public List<DebtInfo> findAllDebtsRelated(@ApiParam("name") @RequestParam String name) {
        try {
            return dao.findAllDebtsRelated(name);
        } catch (UserNotFoundException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST);
        }
    }

    @ApiOperation("Returns all debts")
    @GetMapping("debts")
    public List<DebtInfo> findAllDebts() {
        return dao.findAllDebts();
    }

    @ApiOperation("Change user password")
    @PostMapping("change-password")
    public ResponseEntity<String> changePassword(@RequestBody ChangePasswordDto dto) {
        try {
            securityService.updatePassword(dto);
        } catch (UserNotFoundException e) {
            return new ResponseEntity<>("User not found", HttpStatus.BAD_REQUEST);
        } catch (PasswordsNotMatch e) {
            return new ResponseEntity<>("Passwords don't match", HttpStatus.BAD_REQUEST);
        }
        return new ResponseEntity<>("Password changed successfully!", HttpStatus.OK);
    }

    @ApiOperation("Login page")
    @PostMapping("/login")
    public ResponseEntity<String> authenticateUser(@RequestBody LogInDTO loginDto) {
        securityService.authenticateUser(loginDto);
        return new ResponseEntity<>("User login successfully!", HttpStatus.OK);
    }
}

package ru.m_polukhin.debtsapp.controllers;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import ru.m_polukhin.debtsapp.models.DebtInfo;
import ru.m_polukhin.debtsapp.models.TransactionInfo;
import ru.m_polukhin.debtsapp.services.DebtsDAO;
import ru.m_polukhin.debtsapp.utils.ParseException;
import ru.m_polukhin.debtsapp.utils.UserNotFoundException;

import java.util.List;

@RestController
@Api("Controller to work with Debt Calculation Network")
public class WebController {
    private final DebtsDAO dao;

    @Autowired
    public WebController(DebtsDAO dao) {
        this.dao = dao;
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
    public void addTransaction(@ApiParam("from") @RequestParam String fromName,
                                  @ApiParam("to") @RequestParam String toName,
                                  @ApiParam("sum") @RequestParam Long sum) {
        try {
            dao.addTransaction(fromName, toName, sum);
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
}

package com.medicine.controller;

import com.medicine.common.Result;
import com.medicine.dto.PersonalAccountDTO;
import com.medicine.dto.PersonalBudgetDTO;
import com.medicine.dto.PersonalLedgerDTO;
import com.medicine.dto.PersonalTransactionDTO;
import com.medicine.entity.PersonalAccount;
import com.medicine.entity.PersonalBudget;
import com.medicine.entity.PersonalLedger;
import com.medicine.entity.PersonalTransaction;
import com.medicine.service.PersonalFinanceService;
import com.medicine.service.SysLogService;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import java.time.YearMonth;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/portal/finance")
public class PersonalFinanceController {
    private final PersonalFinanceService financeService;
    private final SysLogService sysLogService;

    public PersonalFinanceController(PersonalFinanceService financeService, SysLogService sysLogService) {
        this.financeService = financeService;
        this.sysLogService = sysLogService;
    }

    @GetMapping("/ledgers")
    public Result<List<PersonalLedger>> ledgers(HttpServletRequest request) {
        return Result.success(financeService.listLedgers(userId(request)));
    }

    @PostMapping("/ledgers")
    public Result<PersonalLedger> createLedger(@Valid @RequestBody PersonalLedgerDTO dto,
                                                HttpServletRequest request) {
        PersonalLedger ledger = financeService.createLedger(userId(request), dto);
        log(request, "创建个人账本", "创建账本：" + ledger.getLedgerName());
        return Result.success(ledger);
    }

    @GetMapping("/accounts")
    public Result<List<PersonalAccount>> accounts(@RequestParam Long ledgerId, HttpServletRequest request) {
        return Result.success(financeService.listAccounts(userId(request), ledgerId));
    }

    @PostMapping("/accounts")
    public Result<PersonalAccount> createAccount(@Valid @RequestBody PersonalAccountDTO dto,
                                                  HttpServletRequest request) {
        PersonalAccount account = financeService.createAccount(userId(request), dto);
        log(request, "创建个人账户", "创建账户：" + account.getAccountName());
        return Result.success(account);
    }

    @DeleteMapping("/accounts")
    public Result<Void> deleteAccount(@RequestParam Long accountId, HttpServletRequest request) {
        financeService.deleteAccount(userId(request), accountId);
        log(request, "删除个人账户", "账户ID：" + accountId);
        return Result.success();
    }

    @GetMapping("/transactions")
    public Result<List<PersonalTransaction>> transactions(@RequestParam Long ledgerId,
                                                           @RequestParam(required = false) String month,
                                                           HttpServletRequest request) {
        return Result.success(financeService.listTransactions(userId(request), ledgerId, month));
    }

    @PostMapping("/transactions")
    public Result<PersonalTransaction> createTransaction(@Valid @RequestBody PersonalTransactionDTO dto,
                                                          HttpServletRequest request) {
        PersonalTransaction transaction = financeService.createTransaction(userId(request), dto);
        log(request, "登记个人收支", transaction.getTransactionType() + "：" + transaction.getAmount());
        return Result.success(transaction);
    }

    @DeleteMapping("/transactions")
    public Result<Void> deleteTransaction(@RequestParam Long transactionId, HttpServletRequest request) {
        financeService.deleteTransaction(userId(request), transactionId);
        log(request, "删除个人流水", "流水ID：" + transactionId);
        return Result.success();
    }

    @GetMapping("/budgets")
    public Result<List<PersonalBudget>> budgets(@RequestParam Long ledgerId,
                                                @RequestParam String month,
                                                HttpServletRequest request) {
        return Result.success(financeService.listBudgets(userId(request), ledgerId, month));
    }

    @PostMapping("/budgets")
    public Result<PersonalBudget> saveBudget(@Valid @RequestBody PersonalBudgetDTO dto,
                                             HttpServletRequest request) {
        PersonalBudget budget = financeService.saveBudget(userId(request), dto);
        log(request, "设置个人预算", budget.getBudgetMonth() + " " + budget.getCategoryName());
        return Result.success(budget);
    }

    @GetMapping("/summary")
    public Result<Map<String, Object>> summary(@RequestParam Long ledgerId,
                                               @RequestParam(required = false) String month,
                                               HttpServletRequest request) {
        String selectedMonth = month == null || month.trim().isEmpty() ? YearMonth.now().toString() : month;
        return Result.success(financeService.monthlySummary(userId(request), ledgerId, selectedMonth));
    }

    @GetMapping("/suggestions")
    public Result<Map<String, Object>> suggestions(@RequestParam Long ledgerId, HttpServletRequest request) {
        return Result.success(financeService.quickSuggestions(userId(request), ledgerId));
    }

    private Long userId(HttpServletRequest request) {
        return (Long) request.getAttribute("userId");
    }

    private void log(HttpServletRequest request, String type, String content) {
        sysLogService.log(userId(request), type, content, request.getRemoteAddr());
    }
}

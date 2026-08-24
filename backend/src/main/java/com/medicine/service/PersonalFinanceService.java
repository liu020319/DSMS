package com.medicine.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.medicine.common.BusinessException;
import com.medicine.dto.PersonalAccountDTO;
import com.medicine.dto.PersonalBudgetDTO;
import com.medicine.dto.PersonalLedgerDTO;
import com.medicine.dto.PersonalTransactionDTO;
import com.medicine.entity.PersonalAccount;
import com.medicine.entity.PersonalBudget;
import com.medicine.entity.PersonalLedger;
import com.medicine.entity.PersonalTransaction;
import com.medicine.mapper.PersonalAccountMapper;
import com.medicine.mapper.PersonalBudgetMapper;
import com.medicine.mapper.PersonalLedgerMapper;
import com.medicine.mapper.PersonalTransactionMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class PersonalFinanceService {
    private static final List<String> ACCOUNT_TYPES = Arrays.asList("CASH", "BANK", "WECHAT", "ALIPAY", "OTHER");
    private static final List<String> TRANSACTION_TYPES = Arrays.asList("INCOME", "EXPENSE");

    private final PersonalLedgerMapper ledgerMapper;
    private final PersonalAccountMapper accountMapper;
    private final PersonalTransactionMapper transactionMapper;
    private final PersonalBudgetMapper budgetMapper;

    public PersonalFinanceService(PersonalLedgerMapper ledgerMapper,
                                  PersonalAccountMapper accountMapper,
                                  PersonalTransactionMapper transactionMapper,
                                  PersonalBudgetMapper budgetMapper) {
        this.ledgerMapper = ledgerMapper;
        this.accountMapper = accountMapper;
        this.transactionMapper = transactionMapper;
        this.budgetMapper = budgetMapper;
    }

    public List<PersonalLedger> listLedgers(Long userId) {
        return ledgerMapper.selectList(new LambdaQueryWrapper<PersonalLedger>()
                .eq(PersonalLedger::getOwnerUserId, userId)
                .eq(PersonalLedger::getStatus, 1)
                .orderByAsc(PersonalLedger::getLedgerId));
    }

    @Transactional
    public PersonalLedger createLedger(Long userId, PersonalLedgerDTO dto) {
        String name = clean(dto.getLedgerName());
        Long duplicate = ledgerMapper.selectCount(new LambdaQueryWrapper<PersonalLedger>()
                .eq(PersonalLedger::getOwnerUserId, userId)
                .eq(PersonalLedger::getLedgerName, name));
        if (duplicate != null && duplicate > 0) throw new BusinessException(409, "同名账本已经存在");

        PersonalLedger ledger = new PersonalLedger();
        ledger.setOwnerUserId(userId);
        ledger.setLedgerName(name);
        ledger.setCurrencyCode("CNY");
        ledger.setStatus(1);
        ledgerMapper.insert(ledger);

        // 账本创建后必须立即可记账，避免“没有付款账户，又无法记第一笔”的死路。
        PersonalAccount defaultAccount = new PersonalAccount();
        defaultAccount.setLedgerId(ledger.getLedgerId());
        defaultAccount.setOwnerUserId(userId);
        defaultAccount.setAccountName("日常账户");
        defaultAccount.setAccountType("WECHAT");
        defaultAccount.setInitialBalance(BigDecimal.ZERO);
        defaultAccount.setStatus(1);
        accountMapper.insert(defaultAccount);
        return ledger;
    }

    public List<PersonalAccount> listAccounts(Long userId, Long ledgerId) {
        requireLedgerOwner(ledgerId, userId);
        return accountMapper.selectList(new LambdaQueryWrapper<PersonalAccount>()
                .eq(PersonalAccount::getOwnerUserId, userId)
                .eq(PersonalAccount::getLedgerId, ledgerId)
                .eq(PersonalAccount::getStatus, 1)
                .orderByAsc(PersonalAccount::getAccountId));
    }

    @Transactional
    public PersonalAccount createAccount(Long userId, PersonalAccountDTO dto) {
        requireLedgerOwner(dto.getLedgerId(), userId);
        String type = dto.getAccountType().trim().toUpperCase();
        if (!ACCOUNT_TYPES.contains(type)) throw new BusinessException(400, "账户类型不正确");

        PersonalAccount account = new PersonalAccount();
        account.setLedgerId(dto.getLedgerId());
        account.setOwnerUserId(userId);
        account.setAccountName(clean(dto.getAccountName()));
        account.setAccountType(type);
        account.setInitialBalance(dto.getInitialBalance());
        account.setStatus(1);
        accountMapper.insert(account);
        return account;
    }

    @Transactional
    public void deleteAccount(Long userId, Long accountId) {
        PersonalAccount account = requireAccountOwner(accountId, userId);
        Long transactionCount = transactionMapper.selectCount(new LambdaQueryWrapper<PersonalTransaction>()
                .eq(PersonalTransaction::getOwnerUserId, userId)
                .eq(PersonalTransaction::getAccountId, accountId));
        if (transactionCount != null && transactionCount > 0) {
            throw new BusinessException(409, "账户已有流水，不能删除；请保留为历史账户");
        }
        account.setStatus(0);
        accountMapper.updateById(account);
    }

    public List<PersonalTransaction> listTransactions(Long userId, Long ledgerId, String month) {
        requireLedgerOwner(ledgerId, userId);
        return queryTransactions(userId, ledgerId, month, true);
    }

    private List<PersonalTransaction> queryTransactions(Long userId, Long ledgerId, String month,
                                                        boolean applyDisplayLimit) {
        LambdaQueryWrapper<PersonalTransaction> wrapper = new LambdaQueryWrapper<PersonalTransaction>()
                .eq(PersonalTransaction::getOwnerUserId, userId)
                .eq(PersonalTransaction::getLedgerId, ledgerId)
                .orderByDesc(PersonalTransaction::getTransactionTime)
                .orderByDesc(PersonalTransaction::getTransactionId);
        if (month != null && !month.trim().isEmpty()) {
            YearMonth yearMonth = parseMonth(month);
            LocalDateTime start = yearMonth.atDay(1).atStartOfDay();
            wrapper.ge(PersonalTransaction::getTransactionTime, start)
                    .lt(PersonalTransaction::getTransactionTime, start.plusMonths(1));
        }
        if (applyDisplayLimit) wrapper.last("LIMIT 500");
        return transactionMapper.selectList(wrapper);
    }

    @Transactional
    public PersonalTransaction createTransaction(Long userId, PersonalTransactionDTO dto) {
        requireLedgerOwner(dto.getLedgerId(), userId);
        PersonalAccount account = requireAccountOwner(dto.getAccountId(), userId);
        if (!dto.getLedgerId().equals(account.getLedgerId())) {
            throw new BusinessException(400, "账户不属于所选账本");
        }
        String type = dto.getTransactionType().trim().toUpperCase();
        if (!TRANSACTION_TYPES.contains(type)) throw new BusinessException(400, "收支类型不正确");

        PersonalTransaction transaction = new PersonalTransaction();
        transaction.setLedgerId(dto.getLedgerId());
        transaction.setAccountId(dto.getAccountId());
        transaction.setOwnerUserId(userId);
        transaction.setTransactionType(type);
        transaction.setCategoryName(clean(dto.getCategoryName()));
        transaction.setAmount(dto.getAmount());
        transaction.setTransactionTime(dto.getTransactionTime());
        transaction.setCounterparty(cleanNullable(dto.getCounterparty()));
        transaction.setNote(cleanNullable(dto.getNote()));
        transactionMapper.insert(transaction);
        return transaction;
    }

    @Transactional
    public void deleteTransaction(Long userId, Long transactionId) {
        PersonalTransaction transaction = transactionMapper.selectById(transactionId);
        if (transaction == null || !userId.equals(transaction.getOwnerUserId())) {
            throw new BusinessException(404, "流水不存在");
        }
        transactionMapper.deleteById(transactionId);
    }

    public List<PersonalBudget> listBudgets(Long userId, Long ledgerId, String month) {
        requireLedgerOwner(ledgerId, userId);
        return budgetMapper.selectList(new LambdaQueryWrapper<PersonalBudget>()
                .eq(PersonalBudget::getOwnerUserId, userId)
                .eq(PersonalBudget::getLedgerId, ledgerId)
                .eq(PersonalBudget::getBudgetMonth, parseMonth(month).toString())
                .orderByAsc(PersonalBudget::getCategoryName));
    }

    @Transactional
    public PersonalBudget saveBudget(Long userId, PersonalBudgetDTO dto) {
        requireLedgerOwner(dto.getLedgerId(), userId);
        String month = parseMonth(dto.getBudgetMonth()).toString();
        String category = clean(dto.getCategoryName());
        PersonalBudget budget = budgetMapper.selectOne(new LambdaQueryWrapper<PersonalBudget>()
                .eq(PersonalBudget::getOwnerUserId, userId)
                .eq(PersonalBudget::getLedgerId, dto.getLedgerId())
                .eq(PersonalBudget::getBudgetMonth, month)
                .eq(PersonalBudget::getCategoryName, category));
        if (budget == null) {
            budget = new PersonalBudget();
            budget.setOwnerUserId(userId);
            budget.setLedgerId(dto.getLedgerId());
            budget.setBudgetMonth(month);
            budget.setCategoryName(category);
            budget.setBudgetAmount(dto.getBudgetAmount());
            budgetMapper.insert(budget);
        } else {
            budget.setBudgetAmount(dto.getBudgetAmount());
            budgetMapper.updateById(budget);
        }
        return budget;
    }

    public Map<String, Object> monthlySummary(Long userId, Long ledgerId, String month) {
        requireLedgerOwner(ledgerId, userId);
        // 汇总不能复用页面列表的 500 条展示上限，否则流水较多时统计会少算。
        List<PersonalTransaction> transactions = queryTransactions(userId, ledgerId, month, false);
        BigDecimal income = BigDecimal.ZERO;
        BigDecimal expense = BigDecimal.ZERO;
        long expenseTransactionCount = 0;
        Map<String, BigDecimal> expenseByCategory = new LinkedHashMap<>();
        Map<String, BigDecimal> expenseByCounterparty = new LinkedHashMap<>();
        Map<String, BigDecimal> expenseByDay = new LinkedHashMap<>();
        PersonalTransaction largestExpense = null;
        for (PersonalTransaction transaction : transactions) {
            if ("INCOME".equals(transaction.getTransactionType())) {
                income = income.add(transaction.getAmount());
            } else if ("EXPENSE".equals(transaction.getTransactionType())) {
                expenseTransactionCount++;
                expense = expense.add(transaction.getAmount());
                expenseByCategory.merge(transaction.getCategoryName(), transaction.getAmount(), BigDecimal::add);
                String counterparty = transaction.getCounterparty() == null ? "未填写去向" : transaction.getCounterparty();
                expenseByCounterparty.merge(counterparty, transaction.getAmount(), BigDecimal::add);
                String day = transaction.getTransactionTime().toLocalDate().toString();
                expenseByDay.merge(day, transaction.getAmount(), BigDecimal::add);
                if (largestExpense == null || transaction.getAmount().compareTo(largestExpense.getAmount()) > 0) {
                    largestExpense = transaction;
                }
            }
        }

        BigDecimal initialBalance = accountMapper.selectList(new LambdaQueryWrapper<PersonalAccount>()
                        .eq(PersonalAccount::getOwnerUserId, userId)
                        .eq(PersonalAccount::getLedgerId, ledgerId)
                        .eq(PersonalAccount::getStatus, 1))
                .stream().map(PersonalAccount::getInitialBalance)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalBudget = listBudgets(userId, ledgerId, month).stream()
                .map(PersonalBudget::getBudgetAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // 当前余额是“初始余额 + 全部历史收入 - 全部历史支出”，不是只算所选月份。
        BigDecimal historicalNet = transactionMapper.selectHistoricalNet(userId, ledgerId);
        if (historicalNet == null) historicalNet = BigDecimal.ZERO;

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("month", parseMonth(month).toString());
        result.put("income", income);
        result.put("expense", expense);
        result.put("net", income.subtract(expense));
        result.put("accountBalance", initialBalance.add(historicalNet));
        result.put("budget", totalBudget);
        result.put("budgetRemaining", totalBudget.subtract(expense));
        result.put("expenseByCategory", expenseByCategory);
        result.put("expenseByCounterparty", expenseByCounterparty);
        result.put("expenseByDay", expenseByDay);
        result.put("averageExpense", expenseTransactionCount == 0 ? BigDecimal.ZERO : expense.divide(
                BigDecimal.valueOf(expenseTransactionCount),
                2, java.math.RoundingMode.HALF_UP));
        if (largestExpense != null) {
            Map<String, Object> largest = new LinkedHashMap<>();
            largest.put("amount", largestExpense.getAmount());
            largest.put("categoryName", largestExpense.getCategoryName());
            largest.put("counterparty", largestExpense.getCounterparty());
            largest.put("transactionTime", largestExpense.getTransactionTime());
            result.put("largestExpense", largest);
        }
        result.put("transactionCount", transactions.size());
        result.put("expenseTransactionCount", expenseTransactionCount);
        return result;
    }

    public Map<String, Object> quickSuggestions(Long userId, Long ledgerId) {
        requireLedgerOwner(ledgerId, userId);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("expenseCategories", safeRows(transactionMapper.selectFrequentCategories(
                userId, ledgerId, "EXPENSE", 24)));
        result.put("incomeCategories", safeRows(transactionMapper.selectFrequentCategories(
                userId, ledgerId, "INCOME", 12)));
        result.put("counterparties", safeRows(transactionMapper.selectFrequentCounterparties(userId, ledgerId, 30)));
        return result;
    }

    private List<Map<String, Object>> safeRows(List<Map<String, Object>> rows) {
        return rows == null ? java.util.Collections.emptyList() : rows;
    }

    private PersonalLedger requireLedgerOwner(Long ledgerId, Long userId) {
        PersonalLedger ledger = ledgerMapper.selectById(ledgerId);
        if (ledger == null || !userId.equals(ledger.getOwnerUserId()) || !Integer.valueOf(1).equals(ledger.getStatus())) {
            throw new BusinessException(404, "账本不存在");
        }
        return ledger;
    }

    private PersonalAccount requireAccountOwner(Long accountId, Long userId) {
        PersonalAccount account = accountMapper.selectById(accountId);
        if (account == null || !userId.equals(account.getOwnerUserId()) || !Integer.valueOf(1).equals(account.getStatus())) {
            throw new BusinessException(404, "账户不存在");
        }
        return account;
    }

    private YearMonth parseMonth(String month) {
        try {
            return YearMonth.parse(month);
        } catch (Exception e) {
            throw new BusinessException(400, "月份格式应为yyyy-MM");
        }
    }

    private String clean(String value) {
        return value == null ? "" : value.trim();
    }

    private String cleanNullable(String value) {
        if (value == null) return null;
        String cleaned = value.trim();
        return cleaned.isEmpty() ? null : cleaned;
    }
}

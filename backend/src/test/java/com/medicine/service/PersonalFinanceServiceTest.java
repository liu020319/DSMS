package com.medicine.service;

import com.medicine.common.BusinessException;
import com.medicine.entity.PersonalAccount;
import com.medicine.entity.PersonalLedger;
import com.medicine.entity.PersonalTransaction;
import com.medicine.mapper.PersonalAccountMapper;
import com.medicine.mapper.PersonalBudgetMapper;
import com.medicine.mapper.PersonalLedgerMapper;
import com.medicine.mapper.PersonalTransactionMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class PersonalFinanceServiceTest {
    private PersonalFinanceService service;
    private PersonalLedgerMapper ledgerMapper;
    private PersonalAccountMapper accountMapper;
    private PersonalTransactionMapper transactionMapper;
    private PersonalBudgetMapper budgetMapper;

    @BeforeEach
    void setUp() {
        ledgerMapper = mock(PersonalLedgerMapper.class);
        accountMapper = mock(PersonalAccountMapper.class);
        transactionMapper = mock(PersonalTransactionMapper.class);
        budgetMapper = mock(PersonalBudgetMapper.class);
        service = new PersonalFinanceService(ledgerMapper, accountMapper, transactionMapper, budgetMapper);
    }

    @Test
    void anotherUsersLedgerIsInvisible() {
        PersonalLedger ledger = ledger(1L, 99L);
        when(ledgerMapper.selectById(1L)).thenReturn(ledger);
        assertThrows(BusinessException.class, () -> service.listAccounts(10L, 1L));
    }

    @Test
    void accountBalanceUsesAllHistoryWhileMonthlyCardsUseSelectedMonth() {
        when(ledgerMapper.selectById(1L)).thenReturn(ledger(1L, 10L));
        PersonalTransaction monthIncome = transaction("INCOME", "100.00");
        PersonalTransaction oldExpense = transaction("EXPENSE", "30.00");
        when(transactionMapper.selectList(any())).thenReturn(Collections.singletonList(monthIncome));
        when(transactionMapper.selectHistoricalNet(10L, 1L)).thenReturn(new BigDecimal("70.00"));
        PersonalAccount account = new PersonalAccount();
        account.setInitialBalance(new BigDecimal("50.00"));
        when(accountMapper.selectList(any())).thenReturn(Collections.singletonList(account));
        when(budgetMapper.selectList(any())).thenReturn(Collections.emptyList());

        Map<String, Object> summary = service.monthlySummary(10L, 1L, "2026-08");

        assertEquals(new BigDecimal("100.00"), summary.get("income"));
        assertEquals(new BigDecimal("120.00"), summary.get("accountBalance"));
        assertEquals(0L, summary.get("expenseTransactionCount"));
    }

    @Test
    void quickSuggestionsAreScopedToCurrentOwnerAndLedger() {
        when(ledgerMapper.selectById(1L)).thenReturn(ledger(1L, 10L));
        when(transactionMapper.selectFrequentCategories(10L, 1L, "EXPENSE", 24))
                .thenReturn(Collections.singletonList(Collections.<String, Object>singletonMap("name", "水果")));
        when(transactionMapper.selectFrequentCategories(10L, 1L, "INCOME", 12))
                .thenReturn(Collections.emptyList());
        when(transactionMapper.selectFrequentCounterparties(10L, 1L, 30))
                .thenReturn(Collections.singletonList(Collections.<String, Object>singletonMap("name", "榴莲摊")));

        Map<String, Object> suggestions = service.quickSuggestions(10L, 1L);

        assertEquals("水果", ((java.util.List<Map<String, Object>>) suggestions.get("expenseCategories")).get(0).get("name"));
        assertEquals("榴莲摊", ((java.util.List<Map<String, Object>>) suggestions.get("counterparties")).get(0).get("name"));
    }

    private PersonalLedger ledger(Long id, Long owner) {
        PersonalLedger ledger = new PersonalLedger();
        ledger.setLedgerId(id); ledger.setOwnerUserId(owner); ledger.setStatus(1);
        return ledger;
    }

    private PersonalTransaction transaction(String type, String amount) {
        PersonalTransaction transaction = new PersonalTransaction();
        transaction.setTransactionType(type); transaction.setCategoryName("测试");
        transaction.setAmount(new BigDecimal(amount));
        transaction.setTransactionTime(LocalDateTime.of(2026, 8, 10, 12, 0));
        return transaction;
    }
}

package com.medicine.service;

import com.medicine.common.BusinessException;
import com.medicine.dto.PurchaseRecordDTO;
import com.medicine.entity.FamilyFundTransaction;
import com.medicine.entity.PurchaseRecord;
import com.medicine.entity.SysUser;
import com.medicine.mapper.FamilyFundTransactionMapper;
import com.medicine.mapper.SysUserMapper;
import com.medicine.service.impl.PurchaseRecordServiceImpl;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PurchaseRecordDirectLedgerTest {
    @Test
    void directPurchaseCreatesNegativeFundEntryAndArchivesEvidence() {
        PurchaseRecordServiceImpl service = new PurchaseRecordServiceImpl();
        FamilyFundTransactionMapper fundMapper = mock(FamilyFundTransactionMapper.class);
        SysUserMapper userMapper = mock(SysUserMapper.class);
        PurchaseEvidenceService evidenceService = mock(PurchaseEvidenceService.class);
        ReflectionTestUtils.setField(service, "fundMapper", fundMapper);
        ReflectionTestUtils.setField(service, "sysUserMapper", userMapper);
        ReflectionTestUtils.setField(service, "purchaseEvidenceService", evidenceService);

        SysUser elder = new SysUser(); elder.setUserId(20L); elder.setBindParentId(10L);
        SysUser operator = new SysUser(); operator.setUserId(10L); operator.setRole("GUARDIAN");
        when(userMapper.selectById(20L)).thenReturn(elder);
        when(userMapper.selectById(10L)).thenReturn(operator);

        PurchaseRecord record = new PurchaseRecord();
        record.setPurchaseId(88L); record.setUserId(20L); record.setOperatorId(10L);
        record.setTotalPrice(new BigDecimal("115.00")); record.setPurchasePlatform("京东");
        record.setPurchaseTime(LocalDateTime.of(2026, 8, 25, 12, 0));
        PurchaseRecordDTO dto = new PurchaseRecordDTO();

        ReflectionTestUtils.invokeMethod(service, "registerDirectPurchaseLedger", record, dto);

        ArgumentCaptor<FamilyFundTransaction> captor = ArgumentCaptor.forClass(FamilyFundTransaction.class);
        verify(fundMapper).insert(captor.capture());
        assertEquals(new BigDecimal("-115.00"), captor.getValue().getAmount());
        assertEquals(88L, captor.getValue().getReferencePurchaseId());
        verify(evidenceService).addForPurchase(record, null, 10L, "GUARDIAN");
    }

    @Test
    void directPurchaseWithoutFamilyBindingIsRejected() {
        PurchaseRecordServiceImpl service = new PurchaseRecordServiceImpl();
        SysUserMapper userMapper = mock(SysUserMapper.class);
        ReflectionTestUtils.setField(service, "fundMapper", mock(FamilyFundTransactionMapper.class));
        ReflectionTestUtils.setField(service, "sysUserMapper", userMapper);
        ReflectionTestUtils.setField(service, "purchaseEvidenceService", mock(PurchaseEvidenceService.class));
        SysUser elder = new SysUser(); elder.setUserId(20L);
        when(userMapper.selectById(20L)).thenReturn(elder);
        PurchaseRecord record = new PurchaseRecord(); record.setUserId(20L);
        assertThrows(BusinessException.class,
                () -> ReflectionTestUtils.invokeMethod(service, "registerDirectPurchaseLedger", record, new PurchaseRecordDTO()));
    }
}

package com.medicine.service;

import com.medicine.common.BusinessException;
import com.medicine.dto.PurchaseEvidenceDTO;
import com.medicine.entity.FamilyPurchaseOrder;
import com.medicine.entity.PurchaseEvidence;
import com.medicine.mapper.FamilyPurchaseOrderMapper;
import com.medicine.mapper.PurchaseEvidenceMapper;
import com.medicine.mapper.SysUserMapper;
import com.medicine.util.AccessControl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PurchaseEvidenceServiceTest {
    private PurchaseEvidenceService service;
    private PurchaseEvidenceMapper evidenceMapper;
    private FileAssetService fileAssetService;
    private NotificationService notificationService;
    private AccessControl accessControl;

    @BeforeEach
    void setUp() {
        evidenceMapper = mock(PurchaseEvidenceMapper.class);
        FamilyPurchaseOrderMapper orderMapper = mock(FamilyPurchaseOrderMapper.class);
        fileAssetService = mock(FileAssetService.class);
        notificationService = mock(NotificationService.class);
        accessControl = mock(AccessControl.class);
        service = new PurchaseEvidenceService(evidenceMapper, orderMapper, fileAssetService,
                notificationService, accessControl, mock(SysUserMapper.class));

        FamilyPurchaseOrder order = new FamilyPurchaseOrder();
        order.setOrderId(100L);
        order.setElderId(20L);
        order.setParentId(10L);
        order.setPurchasePlatform("京东健康");
        when(orderMapper.selectById(100L)).thenReturn(order);
        when(evidenceMapper.insert(any(PurchaseEvidence.class))).thenAnswer(invocation -> {
            PurchaseEvidence evidence = invocation.getArgument(0);
            evidence.setEvidenceId(900L);
            return 1;
        });
    }

    @Test
    void guardianCanAttachPaymentEvidenceToOwnFamilyOrder() {
        PurchaseEvidenceDTO dto = dto("PAYMENT");

        assertEquals(900L, service.add(100L, dto, 10L, "GUARDIAN").getEvidenceId());
        verify(fileAssetService).attachBusiness(88L, "PAYMENT", "PURCHASE_EVIDENCE",
                100L, 10L, 10L, "GUARDIAN");
        verify(notificationService).notify(20L, "订单新增付款凭证",
                "订单100新增了“付款凭证”，可在购药凭证时间线中查看。",
                "PURCHASE_EVIDENCE", 100L);
    }

    @Test
    void guardianCannotAttachEvidenceToAnotherFamilyOrder() {
        assertThrows(BusinessException.class, () -> service.add(100L, dto("PAYMENT"), 11L, "GUARDIAN"));
        verify(fileAssetService, never()).attachBusiness(any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    void unsupportedEvidenceTypeIsRejectedBeforeFileBinding() {
        assertThrows(BusinessException.class, () -> service.add(100L, dto("ID_CARD"), 10L, "GUARDIAN"));
        verify(fileAssetService, never()).attachBusiness(any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    void formerGuardianCannotReadElderTimelineAfterBindingChanged() {
        when(accessControl.isManagedElder(10L, 20L)).thenReturn(false);

        assertThrows(BusinessException.class,
                () -> service.timeline(20L, 2026, 8, 10L, "GUARDIAN"));
    }

    private PurchaseEvidenceDTO dto(String type) {
        PurchaseEvidenceDTO dto = new PurchaseEvidenceDTO();
        dto.setEvidenceType(type);
        dto.setFileId(88L);
        dto.setTitle("付款凭证");
        dto.setOccurredTime(LocalDateTime.of(2026, 8, 22, 10, 30));
        dto.setAmount(new BigDecimal("128.50"));
        return dto;
    }
}

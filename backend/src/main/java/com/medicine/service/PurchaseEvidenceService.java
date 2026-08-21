package com.medicine.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.medicine.common.BusinessException;
import com.medicine.dto.PurchaseEvidenceDTO;
import com.medicine.entity.FamilyPurchaseOrder;
import com.medicine.entity.PurchaseEvidence;
import com.medicine.mapper.FamilyPurchaseOrderMapper;
import com.medicine.mapper.PurchaseEvidenceMapper;
import com.medicine.util.AccessControl;
import com.medicine.vo.PurchaseEvidenceVO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

@Service
public class PurchaseEvidenceService {
    private static final Set<String> TYPES = new HashSet<>(Arrays.asList(
            "CONSULTATION", "PAYMENT", "INVOICE", "ORDER_SCREENSHOT"));
    private static final Map<String, String> TYPE_TEXT = new HashMap<>();

    static {
        TYPE_TEXT.put("CONSULTATION", "问诊记录");
        TYPE_TEXT.put("PAYMENT", "付款凭证");
        TYPE_TEXT.put("INVOICE", "发票凭证");
        TYPE_TEXT.put("ORDER_SCREENSHOT", "下单截图");
    }

    private final PurchaseEvidenceMapper evidenceMapper;
    private final FamilyPurchaseOrderMapper orderMapper;
    private final FileAssetService fileAssetService;
    private final NotificationService notificationService;
    private final AccessControl accessControl;

    public PurchaseEvidenceService(PurchaseEvidenceMapper evidenceMapper,
                                   FamilyPurchaseOrderMapper orderMapper,
                                   FileAssetService fileAssetService,
                                   NotificationService notificationService,
                                   AccessControl accessControl) {
        this.evidenceMapper = evidenceMapper;
        this.orderMapper = orderMapper;
        this.fileAssetService = fileAssetService;
        this.notificationService = notificationService;
        this.accessControl = accessControl;
    }

    @Transactional(rollbackFor = Exception.class)
    public PurchaseEvidenceVO add(Long orderId, PurchaseEvidenceDTO dto,
                                  Long currentUserId, String role) {
        FamilyPurchaseOrder order = requireOrder(orderId);
        requireGuardian(order, currentUserId, role);
        String type = normalizeType(dto.getEvidenceType());
        fileAssetService.attachBusiness(dto.getFileId(), type, "PURCHASE_EVIDENCE",
                orderId, order.getParentId(), currentUserId, role);

        PurchaseEvidence evidence = new PurchaseEvidence();
        evidence.setOrderId(orderId);
        evidence.setElderId(order.getElderId());
        evidence.setParentId(order.getParentId());
        evidence.setEvidenceType(type);
        evidence.setFileId(dto.getFileId());
        evidence.setTitle(defaultTitle(dto.getTitle(), type));
        evidence.setOccurredTime(dto.getOccurredTime());
        evidence.setAmount(dto.getAmount());
        evidence.setPurchasePlatform(blankToDefault(dto.getPurchasePlatform(), order.getPurchasePlatform()));
        evidence.setNote(blankToNull(dto.getNote()));
        evidence.setCreatedBy(currentUserId);
        evidenceMapper.insert(evidence);

        notificationService.notify(order.getElderId(), "订单新增" + TYPE_TEXT.get(type),
                "订单" + orderId + "新增了“" + evidence.getTitle() + "”，可在购药凭证时间线中查看。",
                "PURCHASE_EVIDENCE", orderId);
        return toVO(evidence);
    }

    public List<PurchaseEvidenceVO> listByOrder(Long orderId, Long currentUserId, String role) {
        FamilyPurchaseOrder order = requireOrder(orderId);
        requireFamilyAccess(order, currentUserId, role);
        return toVOList(evidenceMapper.selectList(new LambdaQueryWrapper<PurchaseEvidence>()
                .eq(PurchaseEvidence::getOrderId, orderId)
                .orderByDesc(PurchaseEvidence::getOccurredTime)
                .orderByDesc(PurchaseEvidence::getEvidenceId)));
    }

    public List<PurchaseEvidenceVO> timeline(Long elderId, Integer year, Integer month,
                                             Long currentUserId, String role) {
        if (elderId == null) throw new BusinessException(400, "请选择家庭成员");
        requireCurrentFamilyAccess(elderId, currentUserId, role);
        FamilyPurchaseOrder sample = orderMapper.selectOne(new LambdaQueryWrapper<FamilyPurchaseOrder>()
                .eq(FamilyPurchaseOrder::getElderId, elderId).last("LIMIT 1"));
        if (sample == null) return new ArrayList<>();

        LambdaQueryWrapper<PurchaseEvidence> wrapper = new LambdaQueryWrapper<PurchaseEvidence>()
                .eq(PurchaseEvidence::getElderId, elderId);
        if (year != null) {
            if (year < 2000 || year > 2100) throw new BusinessException(400, "年份范围不正确");
            int normalizedMonth = month == null ? 1 : month;
            if (normalizedMonth < 1 || normalizedMonth > 12) throw new BusinessException(400, "月份范围不正确");
            LocalDateTime start = LocalDateTime.of(year, normalizedMonth, 1, 0, 0);
            LocalDateTime end = month == null ? start.plusYears(1) : start.plusMonths(1);
            wrapper.ge(PurchaseEvidence::getOccurredTime, start)
                    .lt(PurchaseEvidence::getOccurredTime, end);
        } else if (month != null) {
            throw new BusinessException(400, "按月份查询时必须同时选择年份");
        }
        wrapper.orderByDesc(PurchaseEvidence::getOccurredTime)
                .orderByDesc(PurchaseEvidence::getEvidenceId);
        return toVOList(evidenceMapper.selectList(wrapper));
    }

    private FamilyPurchaseOrder requireOrder(Long orderId) {
        FamilyPurchaseOrder order = orderId == null ? null : orderMapper.selectById(orderId);
        if (order == null) throw new BusinessException(404, "订单不存在");
        return order;
    }

    private void requireGuardian(FamilyPurchaseOrder order, Long currentUserId, String role) {
        if ("ADMIN".equals(role)) return;
        if (!"GUARDIAN".equals(role) || currentUserId == null
                || !currentUserId.equals(order.getParentId())) {
            throw new BusinessException(403, "只有该家庭守护人可以登记购药凭证");
        }
    }

    private void requireFamilyAccess(FamilyPurchaseOrder order, Long currentUserId, String role) {
        if ("ADMIN".equals(role)) return;
        if (currentUserId != null && (currentUserId.equals(order.getParentId())
                || currentUserId.equals(order.getElderId()))) return;
        throw new BusinessException(403, "无权查看其他家庭的购药凭证");
    }

    private void requireCurrentFamilyAccess(Long elderId, Long currentUserId, String role) {
        if ("ADMIN".equals(role)) return;
        if ("ELDER".equals(role) && elderId.equals(currentUserId)) return;
        if ("GUARDIAN".equals(role) && accessControl.isManagedElder(currentUserId, elderId)) return;
        throw new BusinessException(403, "无权查看其他家庭的购药凭证");
    }

    private String normalizeType(String value) {
        String type = value == null ? "" : value.trim().toUpperCase(Locale.ROOT);
        if (!TYPES.contains(type)) throw new BusinessException(400, "不支持的凭证类型");
        return type;
    }

    private String defaultTitle(String title, String type) {
        String normalized = blankToNull(title);
        return normalized == null ? TYPE_TEXT.get(type) : normalized;
    }

    private PurchaseEvidenceVO toVO(PurchaseEvidence item) {
        PurchaseEvidenceVO vo = new PurchaseEvidenceVO();
        vo.setEvidenceId(item.getEvidenceId());
        vo.setOrderId(item.getOrderId());
        vo.setElderId(item.getElderId());
        vo.setEvidenceType(item.getEvidenceType());
        vo.setEvidenceTypeText(TYPE_TEXT.get(item.getEvidenceType()));
        vo.setFileId(item.getFileId());
        vo.setFileUrl("/api/files/" + item.getFileId() + "/content");
        vo.setTitle(item.getTitle());
        vo.setOccurredTime(item.getOccurredTime());
        vo.setAmount(item.getAmount());
        vo.setPurchasePlatform(item.getPurchasePlatform());
        vo.setNote(item.getNote());
        vo.setCreatedBy(item.getCreatedBy());
        vo.setCreateTime(item.getCreateTime());
        return vo;
    }

    private List<PurchaseEvidenceVO> toVOList(List<PurchaseEvidence> source) {
        List<PurchaseEvidenceVO> result = new ArrayList<>();
        for (PurchaseEvidence item : source) result.add(toVO(item));
        return result;
    }

    private String blankToNull(String value) {
        return value == null || value.trim().isEmpty() ? null : value.trim();
    }

    private String blankToDefault(String value, String defaultValue) {
        String normalized = blankToNull(value);
        return normalized == null ? defaultValue : normalized;
    }
}

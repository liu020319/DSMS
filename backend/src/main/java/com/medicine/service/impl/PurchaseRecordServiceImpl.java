package com.medicine.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.medicine.common.BusinessException;
import com.medicine.dto.PurchaseRecordDTO;
import com.medicine.entity.Medicine;
import com.medicine.entity.Prescription;
import com.medicine.entity.PurchaseRecord;
import com.medicine.entity.SysUser;
import com.medicine.mapper.MedicineMapper;
import com.medicine.mapper.PrescriptionMapper;
import com.medicine.mapper.PurchaseRecordMapper;
import com.medicine.mapper.SysUserMapper;
import com.medicine.service.PurchaseRecordService;
import com.medicine.service.StockService;
import com.medicine.vo.PurchaseRecordVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class PurchaseRecordServiceImpl extends ServiceImpl<PurchaseRecordMapper, PurchaseRecord> implements PurchaseRecordService {

    @Autowired
    private PrescriptionMapper prescriptionMapper;

    @Autowired
    private MedicineMapper medicineMapper;

    @Autowired
    private SysUserMapper sysUserMapper;

    @Autowired
    private StockService stockService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void addPurchaseRecord(PurchaseRecordDTO dto) {
        validatePurchase(dto);
        PurchaseRecord record = new PurchaseRecord();
        record.setUserId(dto.getUserId());
        record.setPrescriptionId(dto.getPrescriptionId());
        record.setPurchaseDate(dto.getPurchaseDate());
        record.setPurchaseTime(dto.getPurchaseTime() != null ? dto.getPurchaseTime() : dto.getPurchaseDate().atStartOfDay());
        record.setQuantityBoxes(dto.getQuantityBoxes());
        record.setUnitPrice(dto.getUnitPrice());
        record.setTotalPrice(dto.getUnitPrice().multiply(BigDecimal.valueOf(dto.getQuantityBoxes())));
        record.setExpiryDate(dto.getExpiryDate());
        record.setOperatorId(dto.getOperatorId() != null ? dto.getOperatorId() : dto.getUserId());
        record.setPurchasePlatform(dto.getPurchasePlatform());
        record.setPurchaseChannel(dto.getPurchaseChannel());
        record.setOrderId(dto.getOrderId());
        record.setProofUrl(dto.getProofUrl());
        boolean received = dto.getReceiptStatus() != null && dto.getReceiptStatus() == 1;
        record.setReceiptStatus(received ? 1 : 0);
        save(record);
        if (received) {
            stockService.addStockOnPurchase(dto.getPrescriptionId(), dto.getQuantityBoxes(), dto.getExpiryDate());
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updatePurchaseRecord(PurchaseRecordDTO dto) {
        if (dto.getPurchaseId() == null) {
            throw new BusinessException("购药记录ID不能为空");
        }
        PurchaseRecord existing = getById(dto.getPurchaseId());
        if (existing == null) {
            throw new BusinessException("购药记录不存在");
        }
        if (Integer.valueOf(1).equals(existing.getReceiptStatus())) {
            throw new BusinessException("已确认收货的记录已计入库存，不能直接修改");
        }
        if (existing.getOrderId() != null) {
            throw new BusinessException("家庭代购明细由订单和收货核验流程管理，不能在购药记录中单独修改");
        }
        validatePurchase(dto);
        existing.setUserId(dto.getUserId());
        existing.setPrescriptionId(dto.getPrescriptionId());
        existing.setPurchaseDate(dto.getPurchaseDate());
        existing.setPurchaseTime(dto.getPurchaseTime() != null ? dto.getPurchaseTime() : dto.getPurchaseDate().atStartOfDay());
        existing.setQuantityBoxes(dto.getQuantityBoxes());
        existing.setUnitPrice(dto.getUnitPrice());
        existing.setTotalPrice(dto.getUnitPrice().multiply(BigDecimal.valueOf(dto.getQuantityBoxes())));
        existing.setExpiryDate(dto.getExpiryDate());
        if (dto.getPurchasePlatform() != null) {
            existing.setPurchasePlatform(dto.getPurchasePlatform());
        }
        if (dto.getPurchaseChannel() != null) existing.setPurchaseChannel(dto.getPurchaseChannel());
        if (dto.getProofUrl() != null) existing.setProofUrl(dto.getProofUrl());
        updateById(existing);
    }

    @Override
    public void deletePurchaseRecord(Long purchaseId) {
        PurchaseRecord record = getById(purchaseId);
        if (record == null) {
            throw new BusinessException("购药记录不存在");
        }
        if (Integer.valueOf(1).equals(record.getReceiptStatus())) {
            throw new BusinessException("已确认收货的记录已计入库存，不能删除");
        }
        if (record.getOrderId() != null) {
            throw new BusinessException("家庭代购明细不能在购药记录中单独删除");
        }
        removeById(purchaseId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void confirmReceipt(Long purchaseId) {
        confirmReceiptInternal(purchaseId, false);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void confirmFamilyReceipt(Long purchaseId) {
        confirmReceiptInternal(purchaseId, true);
    }

    private void confirmReceiptInternal(Long purchaseId, boolean familyReceiptVerified) {
        PurchaseRecord record = getById(purchaseId);
        if (record == null) {
            throw new BusinessException("购药记录不存在");
        }
        if (record.getReceiptStatus() != null && record.getReceiptStatus() == 1) {
            throw new BusinessException("该记录已确认收货，请勿重复操作");
        }
        if (record.getOrderId() != null && !familyReceiptVerified) {
            throw new BusinessException("家庭代购订单必须由安心用药端完成照片、数量和国药准字号核验");
        }
        record.setReceiptStatus(1);
        updateById(record);
        stockService.addStockOnPurchase(record.getPrescriptionId(), record.getQuantityBoxes(), record.getExpiryDate());
    }

    private void validatePurchase(PurchaseRecordDTO dto) {
        if (dto.getPurchaseDate() == null || dto.getExpiryDate() == null
                || dto.getExpiryDate().isBefore(dto.getPurchaseDate())) {
            throw new BusinessException("有效期不能早于购药日期");
        }
        Prescription prescription = prescriptionMapper.selectById(dto.getPrescriptionId());
        if (prescription == null || !dto.getUserId().equals(prescription.getUserId())) {
            throw new BusinessException("购药用户与用药方案不匹配");
        }
        Medicine medicine = medicineMapper.selectById(prescription.getMedicineId());
        if (medicine == null || !Integer.valueOf(1).equals(medicine.getStatus())) {
            throw new BusinessException("药品不存在或已禁用");
        }
    }

    @Override
    public Page<PurchaseRecordVO> pageList(int current, int size, Long userId, Long prescriptionId, String approvalNumber, List<Long> allowedUserIds) {
        Page<PurchaseRecord> page = new Page<>(current, size);
        if (allowedUserIds != null && allowedUserIds.isEmpty()) {
            Page<PurchaseRecordVO> empty = new Page<>(current, size);
            empty.setTotal(0);
            empty.setRecords(new ArrayList<>());
            return empty;
        }
        LambdaQueryWrapper<PurchaseRecord> wrapper = new LambdaQueryWrapper<>();
        if (userId != null) {
            wrapper.eq(PurchaseRecord::getUserId, userId);
        }
        if (prescriptionId != null) {
            wrapper.eq(PurchaseRecord::getPrescriptionId, prescriptionId);
        }
        if (allowedUserIds != null) wrapper.in(PurchaseRecord::getUserId, allowedUserIds);
        if (approvalNumber != null && !approvalNumber.isEmpty()) {
            LambdaQueryWrapper<Prescription> pWrapper = new LambdaQueryWrapper<>();
            List<Prescription> prescriptions = prescriptionMapper.selectList(pWrapper);
            List<Long> pIds = new ArrayList<>();
            for (Prescription p : prescriptions) {
                Medicine m = medicineMapper.selectById(p.getMedicineId());
                if (m != null && m.getApprovalNumber().contains(approvalNumber)) {
                    pIds.add(p.getPrescriptionId());
                }
            }
            if (pIds.isEmpty()) {
                Page<PurchaseRecordVO> voPage = new Page<>(current, size);
                voPage.setTotal(0);
                voPage.setRecords(new ArrayList<>());
                return voPage;
            }
            wrapper.in(PurchaseRecord::getPrescriptionId, pIds);
        }
        wrapper.orderByDesc(PurchaseRecord::getPurchaseDate);
        page(page, wrapper);
        Page<PurchaseRecordVO> voPage = new Page<>(current, size);
        voPage.setTotal(page.getTotal());
        voPage.setRecords(convertToVOList(page.getRecords()));
        return voPage;
    }

    @Override
    public List<Map<String, Object>> getMonthlyStats(Long userId, List<Long> allowedUserIds) {
        return emptyScope(allowedUserIds) ? new ArrayList<>() : baseMapper.selectMonthlyStatsDynamic(userId, allowedUserIds);
    }

    @Override
    public List<Map<String, Object>> getDailyStats(Long userId, String startDate, List<Long> allowedUserIds) {
        return emptyScope(allowedUserIds) ? new ArrayList<>() : baseMapper.selectDailyStatsDynamic(userId, startDate, allowedUserIds);
    }

    @Override
    public List<Map<String, Object>> getYearlyStats(Long userId, List<Long> allowedUserIds) {
        return emptyScope(allowedUserIds) ? new ArrayList<>() : baseMapper.selectYearlyStatsDynamic(userId, allowedUserIds);
    }

    @Override public List<Map<String, Object>> getWeeklyStats(Long userId, List<Long> allowedUserIds) { return emptyScope(allowedUserIds) ? new ArrayList<>() : baseMapper.selectWeeklyStatsDynamic(userId, allowedUserIds); }
    @Override public List<Map<String, Object>> getPlatformStats(Long userId, List<Long> allowedUserIds) { return emptyScope(allowedUserIds) ? new ArrayList<>() : baseMapper.selectPlatformStatsDynamic(userId, allowedUserIds); }
    @Override public List<Map<String, Object>> getChannelStats(Long userId, List<Long> allowedUserIds) { return emptyScope(allowedUserIds) ? new ArrayList<>() : baseMapper.selectChannelStatsDynamic(userId, allowedUserIds); }
    @Override public List<Map<String, Object>> getTimeBucketStats(Long userId, List<Long> allowedUserIds) { return emptyScope(allowedUserIds) ? new ArrayList<>() : baseMapper.selectTimeBucketStatsDynamic(userId, allowedUserIds); }
    @Override public Map<String, Object> getExpenseSummary(Long userId, List<Long> allowedUserIds) {
        if (!emptyScope(allowedUserIds)) return baseMapper.selectExpenseSummary(userId, allowedUserIds);
        Map<String, Object> empty = new LinkedHashMap<>();
        empty.put("total_amount", BigDecimal.ZERO);
        empty.put("order_count", 0);
        empty.put("average_item_amount", BigDecimal.ZERO);
        empty.put("online_amount", BigDecimal.ZERO);
        empty.put("offline_amount", BigDecimal.ZERO);
        return empty;
    }

    @Override
    public List<PurchaseRecordVO> listForExport(Long userId, List<Long> allowedUserIds) {
        if (emptyScope(allowedUserIds)) return new ArrayList<>();
        LambdaQueryWrapper<PurchaseRecord> wrapper = new LambdaQueryWrapper<>();
        if (userId != null) {
            wrapper.eq(PurchaseRecord::getUserId, userId);
        }
        if (allowedUserIds != null) wrapper.in(PurchaseRecord::getUserId, allowedUserIds);
        wrapper.orderByDesc(PurchaseRecord::getPurchaseDate);
        return convertToVOList(list(wrapper));
    }

    private boolean emptyScope(List<Long> allowedUserIds) {
        return allowedUserIds != null && allowedUserIds.isEmpty();
    }

    private List<PurchaseRecordVO> convertToVOList(List<PurchaseRecord> records) {
        List<PurchaseRecordVO> result = new ArrayList<>();
        for (PurchaseRecord r : records) {
            PurchaseRecordVO vo = new PurchaseRecordVO();
            vo.setPurchaseId(r.getPurchaseId());
            vo.setUserId(r.getUserId());
            vo.setPrescriptionId(r.getPrescriptionId());
            vo.setPurchaseDate(r.getPurchaseDate());
            vo.setPurchaseTime(r.getPurchaseTime());
            vo.setQuantityBoxes(r.getQuantityBoxes());
            vo.setUnitPrice(r.getUnitPrice());
            vo.setTotalPrice(r.getTotalPrice());
            vo.setExpiryDate(r.getExpiryDate());
            vo.setOperatorId(r.getOperatorId());
            vo.setReceiptStatus(r.getReceiptStatus());
            vo.setPurchasePlatform(r.getPurchasePlatform());
            vo.setPurchaseChannel(r.getPurchaseChannel());
            vo.setOrderId(r.getOrderId());
            vo.setProofUrl(r.getProofUrl());
            Prescription p = prescriptionMapper.selectById(r.getPrescriptionId());
            if (p != null) {
                Medicine m = medicineMapper.selectById(p.getMedicineId());
                if (m != null) {
                    vo.setMedicineName(m.getMedicineName());
                    vo.setApprovalNumber(m.getApprovalNumber());
                    vo.setBrandName(m.getBrandName());
                    vo.setSpecification(m.getSpecification());
                }
            }
            SysUser user = sysUserMapper.selectById(r.getUserId());
            if (user != null) {
                vo.setUserName(user.getRealName());
            }
            SysUser operator = sysUserMapper.selectById(r.getOperatorId());
            if (operator != null) {
                vo.setOperatorName(operator.getRealName());
            }
            result.add(vo);
        }
        return result;
    }
}

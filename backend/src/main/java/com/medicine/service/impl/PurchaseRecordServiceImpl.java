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

import java.math.BigDecimal;
import java.util.ArrayList;
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
    public void addPurchaseRecord(PurchaseRecordDTO dto) {
        PurchaseRecord record = new PurchaseRecord();
        record.setUserId(dto.getUserId());
        record.setPrescriptionId(dto.getPrescriptionId());
        record.setPurchaseDate(dto.getPurchaseDate());
        record.setQuantityBoxes(dto.getQuantityBoxes());
        record.setUnitPrice(dto.getUnitPrice());
        record.setTotalPrice(dto.getUnitPrice().multiply(BigDecimal.valueOf(dto.getQuantityBoxes())));
        record.setExpiryDate(dto.getExpiryDate());
        record.setOperatorId(dto.getOperatorId() != null ? dto.getOperatorId() : dto.getUserId());
        record.setPurchasePlatform(dto.getPurchasePlatform());
        boolean received = dto.getReceiptStatus() != null && dto.getReceiptStatus() == 1;
        record.setReceiptStatus(received ? 1 : 0);
        save(record);
        if (received) {
            stockService.addStockOnPurchase(dto.getPrescriptionId(), dto.getQuantityBoxes(), dto.getExpiryDate());
        }
    }

    @Override
    public void updatePurchaseRecord(PurchaseRecordDTO dto) {
        if (dto.getPurchaseId() == null) {
            throw new BusinessException("购药记录ID不能为空");
        }
        PurchaseRecord existing = getById(dto.getPurchaseId());
        if (existing == null) {
            throw new BusinessException("购药记录不存在");
        }
        existing.setPurchaseDate(dto.getPurchaseDate());
        existing.setQuantityBoxes(dto.getQuantityBoxes());
        existing.setUnitPrice(dto.getUnitPrice());
        existing.setTotalPrice(dto.getUnitPrice().multiply(BigDecimal.valueOf(dto.getQuantityBoxes())));
        existing.setExpiryDate(dto.getExpiryDate());
        if (dto.getPurchasePlatform() != null) {
            existing.setPurchasePlatform(dto.getPurchasePlatform());
        }
        updateById(existing);
    }

    @Override
    public void deletePurchaseRecord(Long purchaseId) {
        removeById(purchaseId);
    }

    @Override
    public void confirmReceipt(Long purchaseId) {
        PurchaseRecord record = getById(purchaseId);
        if (record == null) {
            throw new BusinessException("购药记录不存在");
        }
        if (record.getReceiptStatus() != null && record.getReceiptStatus() == 1) {
            throw new BusinessException("该记录已确认收货，请勿重复操作");
        }
        record.setReceiptStatus(1);
        updateById(record);
        stockService.addStockOnPurchase(record.getPrescriptionId(), record.getQuantityBoxes(), record.getExpiryDate());
    }

    @Override
    public Page<PurchaseRecordVO> pageList(int current, int size, Long userId, Long prescriptionId, String approvalNumber) {
        Page<PurchaseRecord> page = new Page<>(current, size);
        LambdaQueryWrapper<PurchaseRecord> wrapper = new LambdaQueryWrapper<>();
        if (userId != null) {
            wrapper.eq(PurchaseRecord::getUserId, userId);
        }
        if (prescriptionId != null) {
            wrapper.eq(PurchaseRecord::getPrescriptionId, prescriptionId);
        }
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
    public List<Map<String, Object>> getMonthlyStats(Long userId) {
        return baseMapper.selectMonthlyStatsDynamic(userId);
    }

    @Override
    public List<Map<String, Object>> getDailyStats(Long userId, String startDate) {
        return baseMapper.selectDailyStatsDynamic(userId, startDate);
    }

    @Override
    public List<Map<String, Object>> getYearlyStats(Long userId) {
        return baseMapper.selectYearlyStatsDynamic(userId);
    }

    @Override
    public List<PurchaseRecordVO> listForExport(Long userId) {
        LambdaQueryWrapper<PurchaseRecord> wrapper = new LambdaQueryWrapper<>();
        if (userId != null) {
            wrapper.eq(PurchaseRecord::getUserId, userId);
        }
        wrapper.orderByDesc(PurchaseRecord::getPurchaseDate);
        return convertToVOList(list(wrapper));
    }

    private List<PurchaseRecordVO> convertToVOList(List<PurchaseRecord> records) {
        List<PurchaseRecordVO> result = new ArrayList<>();
        for (PurchaseRecord r : records) {
            PurchaseRecordVO vo = new PurchaseRecordVO();
            vo.setPurchaseId(r.getPurchaseId());
            vo.setUserId(r.getUserId());
            vo.setPrescriptionId(r.getPrescriptionId());
            vo.setPurchaseDate(r.getPurchaseDate());
            vo.setQuantityBoxes(r.getQuantityBoxes());
            vo.setUnitPrice(r.getUnitPrice());
            vo.setTotalPrice(r.getTotalPrice());
            vo.setExpiryDate(r.getExpiryDate());
            vo.setOperatorId(r.getOperatorId());
            vo.setReceiptStatus(r.getReceiptStatus());
            vo.setPurchasePlatform(r.getPurchasePlatform());
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

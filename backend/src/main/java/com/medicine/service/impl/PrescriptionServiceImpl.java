package com.medicine.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.medicine.common.BusinessException;
import com.medicine.common.TakeFrequencyEnum;
import com.medicine.dto.PrescriptionDTO;
import com.medicine.entity.Medicine;
import com.medicine.entity.Prescription;
import com.medicine.entity.PrescriptionHistory;
import com.medicine.entity.Stock;
import com.medicine.entity.SysUser;
import com.medicine.mapper.MedicineMapper;
import com.medicine.mapper.PrescriptionHistoryMapper;
import com.medicine.mapper.PrescriptionMapper;
import com.medicine.mapper.StockMapper;
import com.medicine.service.PrescriptionService;
import com.medicine.service.StockService;
import com.medicine.util.MedicationCalcUtil;
import com.medicine.vo.PrescriptionVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
public class PrescriptionServiceImpl extends ServiceImpl<PrescriptionMapper, Prescription> implements PrescriptionService {

    @Autowired
    private MedicineMapper medicineMapper;

    @Autowired
    private PrescriptionHistoryMapper historyMapper;

    @Autowired
    private StockMapper stockMapper;

    @Autowired
    private com.medicine.mapper.SysUserMapper sysUserMapper;

    @Autowired
    private StockService stockService;

    @Override
    public void addPrescription(PrescriptionDTO dto) {
        LambdaQueryWrapper<Prescription> dupWrapper = new LambdaQueryWrapper<>();
        dupWrapper.eq(Prescription::getUserId, dto.getUserId())
                  .eq(Prescription::getMedicineId, dto.getMedicineId())
                  .eq(Prescription::getStatus, 1);
        long count = count(dupWrapper);
        if (count > 0) {
            throw new BusinessException("该用户已有此药品的用药方案，无法重复添加");
        }

        Medicine medicine = medicineMapper.selectById(dto.getMedicineId());
        if (medicine == null) {
            throw new BusinessException("药品不存在");
        }
        Prescription prescription = new Prescription();
        prescription.setUserId(dto.getUserId());
        prescription.setMedicineId(dto.getMedicineId());

        // 根据服用频次枚举自动设置每日次数和服用时段
        String frequencyCode = dto.getTakeFrequencyCode();
        if (frequencyCode != null && !frequencyCode.isEmpty()) {
            TakeFrequencyEnum frequency = TakeFrequencyEnum.fromCode(frequencyCode);
            prescription.setTakeFrequencyCode(frequencyCode);
            prescription.setTakePeriods(MedicationCalcUtil.toJsonArray(frequency.getPeriods()));
            prescription.setDailyTimes(frequency.getDailyTimes());
            // 自动推断takeTiming
            prescription.setTakeTiming(inferTakeTiming(frequency));
        } else {
            prescription.setDailyTimes(dto.getDailyTimes() != null ? dto.getDailyTimes() : 1);
            String inferredCode = inferFrequencyCode(prescription.getDailyTimes(), dto.getTakeTiming());
            prescription.setTakeFrequencyCode(inferredCode);
            TakeFrequencyEnum frequency = TakeFrequencyEnum.fromCode(inferredCode);
            if (frequency != null) {
                prescription.setTakePeriods(MedicationCalcUtil.toJsonArray(frequency.getPeriods()));
            } else {
                prescription.setTakePeriods("[\"MORNING\"]");
            }
            prescription.setTakeTiming(dto.getTakeTiming() != null ? dto.getTakeTiming() : "每晨");
        }

        prescription.setDosagePerTime(dto.getDosagePerTime());
        prescription.setDailyConsumption(prescription.getDailyTimes() * dto.getDosagePerTime());
        if (prescription.getDailyConsumption() > 0) {
            prescription.setDaysPerBox(MedicationCalcUtil.calcDaysPerBox(medicine.getUnitPerBox(), prescription.getDailyConsumption()));
        } else {
            prescription.setDaysPerBox(0);
        }
        prescription.setTakeNotes(dto.getTakeNotes());
        prescription.setDosageUnit(dto.getDosageUnit() != null ? dto.getDosageUnit() : "片");
        prescription.setStatus(1);
        save(prescription);
    }

    @Override
    public void updatePrescription(PrescriptionDTO dto, String changeReason) {
        if (dto.getPrescriptionId() == null) {
            throw new BusinessException("方案ID不能为空");
        }
        Prescription existing = getById(dto.getPrescriptionId());
        if (existing == null) {
            throw new BusinessException("用药方案不存在");
        }
        saveHistory(existing, changeReason);
        Medicine medicine = medicineMapper.selectById(dto.getMedicineId());
        existing.setMedicineId(dto.getMedicineId());

        String frequencyCode = dto.getTakeFrequencyCode();
        if (frequencyCode != null && !frequencyCode.isEmpty()) {
            TakeFrequencyEnum frequency = TakeFrequencyEnum.fromCode(frequencyCode);
            existing.setTakeFrequencyCode(frequencyCode);
            existing.setTakePeriods(MedicationCalcUtil.toJsonArray(frequency.getPeriods()));
            existing.setDailyTimes(frequency.getDailyTimes());
            existing.setTakeTiming(inferTakeTiming(frequency));
        } else {
            existing.setDailyTimes(dto.getDailyTimes() != null ? dto.getDailyTimes() : 1);
            String inferredCode = inferFrequencyCode(existing.getDailyTimes(), dto.getTakeTiming());
            existing.setTakeFrequencyCode(inferredCode);
            TakeFrequencyEnum frequency = TakeFrequencyEnum.fromCode(inferredCode);
            if (frequency != null) {
                existing.setTakePeriods(MedicationCalcUtil.toJsonArray(frequency.getPeriods()));
            } else {
                existing.setTakePeriods("[\"MORNING\"]");
            }
            existing.setTakeTiming(dto.getTakeTiming() != null ? dto.getTakeTiming() : "每晨");
        }

        existing.setDosagePerTime(dto.getDosagePerTime());
        existing.setDailyConsumption(existing.getDailyTimes() * dto.getDosagePerTime());
        if (medicine != null && existing.getDailyConsumption() > 0) {
            existing.setDaysPerBox(MedicationCalcUtil.calcDaysPerBox(medicine.getUnitPerBox(), existing.getDailyConsumption()));
        }
        existing.setTakeNotes(dto.getTakeNotes());
        if (dto.getDosageUnit() != null) {
            existing.setDosageUnit(dto.getDosageUnit());
        }
        updateById(existing);
        Stock stock = stockService.getByPrescriptionId(existing.getPrescriptionId());
        if (stock != null) {
            stock.setLastCalcTime(LocalDateTime.now());
            stockMapper.updateById(stock);
            stockService.calculateRemainingDays(stock.getStockId());
        }
    }

    @Override
    public void stopPrescription(Long prescriptionId) {
        Prescription prescription = getById(prescriptionId);
        if (prescription == null) {
            throw new BusinessException("用药方案不存在");
        }
        prescription.setStatus(0);
        updateById(prescription);
    }

    @Override
    public void enablePrescription(Long prescriptionId) {
        Prescription prescription = getById(prescriptionId);
        if (prescription == null) {
            throw new BusinessException("用药方案不存在");
        }
        prescription.setStatus(1);
        updateById(prescription);
    }

    @Override
    public Page<PrescriptionVO> pageList(int current, int size, Long userId, Long medicineId, String realName) {
        Page<Prescription> page = new Page<>(current, size);
        if (realName != null && !realName.trim().isEmpty()) {
            Page<Prescription> result = baseMapper.selectPageWithRealName(page, userId, medicineId, realName.trim());
            Page<PrescriptionVO> voPage = new Page<>(current, size);
            voPage.setTotal(result.getTotal());
            voPage.setRecords(convertToVOList(result.getRecords()));
            return voPage;
        }
        LambdaQueryWrapper<Prescription> wrapper = new LambdaQueryWrapper<>();
        if (userId != null) {
            wrapper.eq(Prescription::getUserId, userId);
        }
        if (medicineId != null) {
            wrapper.eq(Prescription::getMedicineId, medicineId);
        }
        wrapper.orderByDesc(Prescription::getCreateTime);
        page(page, wrapper);
        Page<PrescriptionVO> voPage = new Page<>(current, size);
        voPage.setTotal(page.getTotal());
        voPage.setRecords(convertToVOList(page.getRecords()));
        return voPage;
    }

    @Override
    public List<PrescriptionVO> listByUserId(Long userId) {
        LambdaQueryWrapper<Prescription> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Prescription::getUserId, userId)
               .eq(Prescription::getStatus, 1)
               .orderByDesc(Prescription::getCreateTime);
        return convertToVOList(list(wrapper));
    }

    @Override
    public PrescriptionVO getDetail(Long prescriptionId) {
        Prescription prescription = getById(prescriptionId);
        if (prescription == null) {
            throw new BusinessException("用药方案不存在");
        }
        List<PrescriptionVO> vos = convertToVOList(java.util.Collections.singletonList(prescription));
        return vos.isEmpty() ? null : vos.get(0);
    }

    @Override
    public List<PrescriptionVO> getHistory(Long prescriptionId) {
        LambdaQueryWrapper<PrescriptionHistory> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(PrescriptionHistory::getPrescriptionId, prescriptionId)
               .orderByDesc(PrescriptionHistory::getCreateTime);
        List<PrescriptionHistory> histories = historyMapper.selectList(wrapper);
        List<PrescriptionVO> result = new ArrayList<>();
        for (PrescriptionHistory h : histories) {
            PrescriptionVO vo = new PrescriptionVO();
            vo.setPrescriptionId(h.getPrescriptionId());
            vo.setUserId(h.getUserId());
            vo.setMedicineId(h.getMedicineId());
            vo.setDailyTimes(h.getDailyTimes());
            vo.setDosagePerTime(h.getDosagePerTime());
            vo.setDailyConsumption(h.getDailyConsumption());
            vo.setDaysPerBox(h.getDaysPerBox());
            vo.setTakeNotes(h.getTakeNotes());
            vo.setCreateTime(h.getCreateTime());
            Medicine medicine = medicineMapper.selectById(h.getMedicineId());
            if (medicine != null) {
                vo.setMedicineName(medicine.getMedicineName());
                vo.setApprovalNumber(medicine.getApprovalNumber());
                vo.setBrandName(medicine.getBrandName());
                vo.setSpecification(medicine.getSpecification());
                vo.setUnitPerBox(medicine.getUnitPerBox());
                vo.setReferencePrice(medicine.getReferencePrice());
                vo.setManufacturer(medicine.getManufacturer());
            }
            result.add(vo);
        }
        return result;
    }

    private void saveHistory(Prescription prescription, String changeReason) {
        PrescriptionHistory history = new PrescriptionHistory();
        history.setPrescriptionId(prescription.getPrescriptionId());
        history.setUserId(prescription.getUserId());
        history.setMedicineId(prescription.getMedicineId());
        history.setDailyTimes(prescription.getDailyTimes());
        history.setDosagePerTime(prescription.getDosagePerTime());
        history.setDailyConsumption(prescription.getDailyConsumption());
        history.setDaysPerBox(prescription.getDaysPerBox());
        history.setTakeNotes(prescription.getTakeNotes());
        history.setChangeReason(changeReason);
        historyMapper.insert(history);
    }

    private List<PrescriptionVO> convertToVOList(List<Prescription> prescriptions) {
        List<PrescriptionVO> result = new ArrayList<>();
        for (Prescription p : prescriptions) {
            PrescriptionVO vo = new PrescriptionVO();
            vo.setPrescriptionId(p.getPrescriptionId());
            vo.setUserId(p.getUserId());
            vo.setMedicineId(p.getMedicineId());
            vo.setDailyTimes(p.getDailyTimes());
            vo.setDosagePerTime(p.getDosagePerTime());
            vo.setDosageUnit(p.getDosageUnit());
            vo.setDailyConsumption(p.getDailyConsumption());
            vo.setDaysPerBox(p.getDaysPerBox());
            vo.setTakeNotes(p.getTakeNotes());
            vo.setTakeTiming(p.getTakeTiming());
            vo.setTakeFrequencyCode(p.getTakeFrequencyCode());
            vo.setTakeFrequencyLabel(TakeFrequencyEnum.getLabelByCode(p.getTakeFrequencyCode()));
            vo.setTakePeriods(p.getTakePeriods());
            vo.setStatus(p.getStatus());
            vo.setCreateTime(p.getCreateTime());
            vo.setUpdateTime(p.getUpdateTime());
            Medicine medicine = medicineMapper.selectById(p.getMedicineId());
            if (medicine != null) {
                vo.setMedicineName(medicine.getMedicineName());
                vo.setApprovalNumber(medicine.getApprovalNumber());
                vo.setBrandName(medicine.getBrandName());
                vo.setSpecification(medicine.getSpecification());
                vo.setUnitPerBox(medicine.getUnitPerBox());
                vo.setReferencePrice(medicine.getReferencePrice());
                vo.setManufacturer(medicine.getManufacturer());
                vo.setImageUrl(medicine.getImageUrl());
            }
            SysUser user = sysUserMapper.selectById(p.getUserId());
            if (user != null) {
                vo.setRealName(user.getRealName());
                vo.setRole(user.getRole());
            }
            Stock stock = stockService.getByPrescriptionId(p.getPrescriptionId());
            if (stock != null) {
                vo.setTotalRemainingUnits(stock.getTotalRemainingUnits());
                vo.setRemainingDays(stock.getRemainingDays());
                vo.setExpiryDate(stock.getExpiryDate());
            }
            result.add(vo);
        }
        return result;
    }

    /**
     * 根据服用频次推断takeTiming文本
     */
    private String inferTakeTiming(TakeFrequencyEnum frequency) {
        switch (frequency) {
            case DAILY_1_MORNING: return "每晨";
            case DAILY_1_NOON: return "午服";
            case DAILY_1_EVENING: return "晚间";
            case DAILY_2_MORNING_EVENING: return "早晚";
            case DAILY_3_FULL_DAY: return "一日三次";
            default: return "";
        }
    }

    /**
     * 根据dailyTimes和takeTiming反推服用频次枚举值
     */
    private String inferFrequencyCode(int dailyTimes, String takeTiming) {
        if (dailyTimes == 3) return TakeFrequencyEnum.DAILY_3_FULL_DAY.getCode();
        if (dailyTimes == 2) return TakeFrequencyEnum.DAILY_2_MORNING_EVENING.getCode();
        if (dailyTimes == 1) {
            if (takeTiming != null) {
                if (takeTiming.contains("晚") || takeTiming.contains("睡")) {
                    return TakeFrequencyEnum.DAILY_1_EVENING.getCode();
                }
                if (takeTiming.contains("午")) {
                    return TakeFrequencyEnum.DAILY_1_NOON.getCode();
                }
            }
            return TakeFrequencyEnum.DAILY_1_MORNING.getCode();
        }
        return TakeFrequencyEnum.DAILY_1_MORNING.getCode();
    }
}

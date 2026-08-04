package com.medicine.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.medicine.common.BusinessException;
import com.medicine.common.BusinessCode;
import com.medicine.entity.Medicine;
import com.medicine.entity.Prescription;
import com.medicine.entity.Stock;
import com.medicine.mapper.MedicineMapper;
import com.medicine.mapper.PrescriptionMapper;
import com.medicine.mapper.StockMapper;
import com.medicine.service.StockService;
import com.medicine.vo.StockVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class StockServiceImpl extends ServiceImpl<StockMapper, Stock> implements StockService {

    @Autowired
    private PrescriptionMapper prescriptionMapper;

    @Autowired
    private MedicineMapper medicineMapper;

    @Override
    public void calculateRemainingDays(Long stockId) {
        Stock stock = getById(stockId);
        if (stock == null) {
            return;
        }
        Prescription prescription = prescriptionMapper.selectById(stock.getPrescriptionId());
        if (prescription == null || prescription.getStatus() == 0) {
            return;
        }
        int dailyConsumption = prescription.getDailyConsumption();

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime lastCalcTime = stock.getLastCalcTime();

        long n = ChronoUnit.DAYS.between(lastCalcTime.toLocalDate(), now.toLocalDate());
        if (n < 0) {
            n = 0;
        }

        int consumed = (int) (n * dailyConsumption);
        int remaining = stock.getTotalRemainingUnits() - consumed;
        if (remaining < 0) {
            remaining = 0;
        }

        int remainingDays = 0;
        if (dailyConsumption > 0) {
            remainingDays = remaining / dailyConsumption;
            if (remainingDays < 0) {
                remainingDays = 0;
            }
        }

        stock.setTotalRemainingUnits(remaining);
        stock.setRemainingDays(remainingDays);
        stock.setLastCalcTime(now);
        updateById(stock);

        log.info("实时计算完成: stockId={}, 已过天数={}, 已消耗={}, 剩余={}, 剩余天数={}",
                stockId, n, consumed, remaining, remainingDays);
    }

    @Override
    public void calculateAllByUserId(Long userId) {
        LambdaQueryWrapper<Prescription> pWrapper = new LambdaQueryWrapper<>();
        pWrapper.eq(Prescription::getUserId, userId)
                .eq(Prescription::getStatus, 1);
        List<Prescription> prescriptions = prescriptionMapper.selectList(pWrapper);
        for (Prescription p : prescriptions) {
            Stock stock = getByPrescriptionId(p.getPrescriptionId());
            if (stock != null) {
                calculateRemainingDays(stock.getStockId());
            }
        }
    }

    @Override
    public void calculateAll() {
        List<Stock> stocks = list();
        for (Stock stock : stocks) {
            calculateRemainingDays(stock.getStockId());
        }
    }

    @Override
    public void addStockOnPurchase(Long prescriptionId, Integer quantityBoxes, LocalDate expiryDate) {
        Prescription prescription = prescriptionMapper.selectById(prescriptionId);
        if (prescription == null) {
            throw new BusinessException("用药方案不存在");
        }
        Medicine medicine = medicineMapper.selectById(prescription.getMedicineId());
        int addedUnits = quantityBoxes * medicine.getUnitPerBox();

        Stock stock = getByPrescriptionId(prescriptionId);
        if (stock == null) {
            stock = new Stock();
            stock.setPrescriptionId(prescriptionId);
            stock.setLastCalcTime(LocalDateTime.now());
            stock.setTotalRemainingUnits(addedUnits);
            stock.setRemainingDays(addedUnits / prescription.getDailyConsumption());
            stock.setExpiryDate(expiryDate);
            save(stock);
        } else {
            calculateRemainingDays(stock.getStockId());
            stock = getById(stock.getStockId());
            stock.setTotalRemainingUnits(stock.getTotalRemainingUnits() + addedUnits);
            if (expiryDate.isBefore(stock.getExpiryDate())) {
                stock.setExpiryDate(expiryDate);
            }
            if (prescription.getDailyConsumption() > 0) {
                stock.setRemainingDays(stock.getTotalRemainingUnits() / prescription.getDailyConsumption());
            }
            stock.setLastCalcTime(LocalDateTime.now());
            updateById(stock);
        }
    }

    @Override
    public void deductStockOnLoss(Long prescriptionId, Integer lossBoxes) {
        Prescription prescription = prescriptionMapper.selectById(prescriptionId);
        if (prescription == null) {
            throw new BusinessException("用药方案不存在");
        }
        Medicine medicine = medicineMapper.selectById(prescription.getMedicineId());
        int deductUnits = lossBoxes * medicine.getUnitPerBox();

        Stock stock = getByPrescriptionId(prescriptionId);
        if (stock == null) {
            throw new BusinessException("库存记录不存在");
        }
        calculateRemainingDays(stock.getStockId());
        stock = getById(stock.getStockId());
        int newRemaining = stock.getTotalRemainingUnits() - deductUnits;
        if (newRemaining < 0) {
            throw new BusinessException(BusinessCode.STOCK_NEGATIVE);
        }
        stock.setTotalRemainingUnits(newRemaining);
        if (prescription.getDailyConsumption() > 0) {
            stock.setRemainingDays(newRemaining / prescription.getDailyConsumption());
        }
        stock.setLastCalcTime(LocalDateTime.now());
        updateById(stock);
    }

    @Override
    public Stock getByPrescriptionId(Long prescriptionId) {
        LambdaQueryWrapper<Stock> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Stock::getPrescriptionId, prescriptionId);
        return getOne(wrapper);
    }

    @Override
    public List<StockVO> getWarningList(Long userId) {
        List<Map<String, Object>> details;
        if (userId != null) {
            details = baseMapper.selectStockWithDetailByUserId(userId);
        } else {
            details = baseMapper.selectStockWithDetail();
        }
        List<StockVO> result = new ArrayList<>();
        for (Map<String, Object> detail : details) {
            StockVO vo = convertToVO(detail);
            if (vo.getRemainingDays() < 7) {
                vo.setIsWarning(true);
                result.add(vo);
            }
        }
        return result;
    }

    @Override
    public List<StockVO> getExpiringList(Long userId) {
        List<Map<String, Object>> details;
        if (userId != null) {
            details = baseMapper.selectStockWithDetailByUserId(userId);
        } else {
            details = baseMapper.selectStockWithDetail();
        }
        List<StockVO> result = new ArrayList<>();
        LocalDate thirtyDaysLater = LocalDate.now().plusDays(30);
        for (Map<String, Object> detail : details) {
            StockVO vo = convertToVO(detail);
            if (vo.getExpiryDate() != null && !vo.getExpiryDate().isAfter(thirtyDaysLater)) {
                vo.setIsExpiring(true);
                result.add(vo);
            }
        }
        return result;
    }

    @Override
    public List<StockVO> getAllStockDetail(Long userId) {
        List<Map<String, Object>> details;
        if (userId != null) {
            details = baseMapper.selectStockWithDetailByUserId(userId);
        } else {
            details = baseMapper.selectStockWithDetail();
        }
        List<StockVO> result = new ArrayList<>();
        LocalDate thirtyDaysLater = LocalDate.now().plusDays(30);
        for (Map<String, Object> detail : details) {
            StockVO vo = convertToVO(detail);
            vo.setIsWarning(vo.getRemainingDays() < 7);
            vo.setIsExpiring(vo.getExpiryDate() != null && !vo.getExpiryDate().isAfter(thirtyDaysLater));
            result.add(vo);
        }
        return result;
    }

    @Override
    public int calculateBoxCount(Long prescriptionId, Integer days) {
        Prescription prescription = prescriptionMapper.selectById(prescriptionId);
        if (prescription == null) {
            throw new BusinessException("用药方案不存在");
        }
        Medicine medicine = medicineMapper.selectById(prescription.getMedicineId());
        if (medicine == null) {
            throw new BusinessException("药品不存在");
        }
        int neededUnits = days * prescription.getDailyConsumption();
        return (int) Math.ceil((double) neededUnits / medicine.getUnitPerBox());
    }

    private StockVO convertToVO(Map<String, Object> detail) {
        StockVO vo = new StockVO();
        vo.setStockId(((Number) detail.get("stock_id")).longValue());
        vo.setPrescriptionId(((Number) detail.get("prescription_id")).longValue());
        vo.setTotalRemainingUnits(((Number) detail.get("total_remaining_units")).intValue());
        vo.setRemainingDays(((Number) detail.get("remaining_days")).intValue());
        vo.setDailyConsumption(((Number) detail.get("daily_consumption")).intValue());
        vo.setUserId(((Number) detail.get("user_id")).longValue());
        vo.setMedicineId(((Number) detail.get("medicine_id")).longValue());
        vo.setMedicineName((String) detail.get("medicine_name"));
        vo.setApprovalNumber((String) detail.get("approval_number"));
        vo.setBrandName((String) detail.get("brand_name"));
        vo.setSpecification((String) detail.get("specification"));
        vo.setUnitPerBox(((Number) detail.get("unit_per_box")).intValue());

        // 分时段扣减新增字段
        Object todayDeductedPeriods = detail.get("today_deducted_periods");
        if (todayDeductedPeriods != null) {
            vo.setTodayDeductedPeriods(todayDeductedPeriods.toString());
        }
        Object lastDeductionDate = detail.get("last_deduction_date");
        if (lastDeductionDate instanceof java.sql.Date) {
            vo.setLastDeductionDate(((java.sql.Date) lastDeductionDate).toLocalDate());
        } else if (lastDeductionDate instanceof LocalDate) {
            vo.setLastDeductionDate((LocalDate) lastDeductionDate);
        }
        Object version = detail.get("version");
        if (version != null) {
            vo.setVersion(((Number) version).intValue());
        }
        Object takeFrequencyCode = detail.get("take_frequency_code");
        if (takeFrequencyCode != null) {
            vo.setTakeFrequencyCode(takeFrequencyCode.toString());
            vo.setTakeFrequencyLabel(com.medicine.common.TakeFrequencyEnum.getLabelByCode(takeFrequencyCode.toString()));
        }
        Object takePeriods = detail.get("take_periods");
        if (takePeriods != null) {
            vo.setTakePeriods(takePeriods.toString());
        }
        Object dosagePerTime = detail.get("dosage_per_time");
        if (dosagePerTime != null) {
            vo.setDosagePerTime(((Number) dosagePerTime).intValue());
        }
        Object dosageUnit = detail.get("dosage_unit");
        if (dosageUnit != null) {
            vo.setDosageUnit(dosageUnit.toString());
        }
        Object takeTiming = detail.get("take_timing");
        if (takeTiming != null) {
            vo.setTakeTiming(takeTiming.toString());
        }

        Object realName = detail.get("real_name");
        if (realName != null) {
            vo.setRealName((String) realName);
        }
        Object lastCalcTime = detail.get("last_calc_time");
        if (lastCalcTime instanceof java.sql.Timestamp) {
            vo.setLastCalcTime(((java.sql.Timestamp) lastCalcTime).toLocalDateTime());
        } else if (lastCalcTime instanceof LocalDateTime) {
            vo.setLastCalcTime((LocalDateTime) lastCalcTime);
        }
        Object expiryDate = detail.get("expiry_date");
        if (expiryDate instanceof java.sql.Date) {
            vo.setExpiryDate(((java.sql.Date) expiryDate).toLocalDate());
        } else if (expiryDate instanceof LocalDate) {
            vo.setExpiryDate((LocalDate) expiryDate);
        }
        return vo;
    }
}

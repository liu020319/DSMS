package com.medicine.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.medicine.common.BusinessException;
import com.medicine.entity.Medicine;
import com.medicine.entity.Prescription;
import com.medicine.entity.Stock;
import com.medicine.mapper.MedicineMapper;
import com.medicine.mapper.PrescriptionMapper;
import com.medicine.mapper.StockMapper;
import com.medicine.service.StockDeductionService;
import com.medicine.service.SysConfigService;
import com.medicine.service.SysLogService;
import com.medicine.util.MedicationCalcUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

@Slf4j
@Service
public class StockDeductionServiceImpl extends ServiceImpl<StockMapper, Stock> implements StockDeductionService {

    @Autowired
    private PrescriptionMapper prescriptionMapper;

    @Autowired
    private MedicineMapper medicineMapper;

    @Autowired
    private SysConfigService sysConfigService;

    @Autowired
    private SysLogService sysLogService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public MedicationCalcUtil.PeriodDeductResult deductOnLoginWithPeriod(Long stockId) {
        Stock stock = getById(stockId);
        if (stock == null) {
            return null;
        }
        Prescription prescription = prescriptionMapper.selectById(stock.getPrescriptionId());
        if (prescription == null || prescription.getStatus() == 0) {
            return null;
        }

        // 过期药品拦截：已过期的药品不参与任何扣减计算
        if (stock.getExpiryDate() != null && stock.getExpiryDate().isBefore(LocalDate.now())) {
            log.info("药品已过期，跳过扣减: stockId={}, expiryDate={}", stockId, stock.getExpiryDate());
            return null;
        }

        // 获取时段阈值配置
        LocalTime morningThreshold = parseTimeConfig("morning_threshold", MedicationCalcUtil.DEFAULT_MORNING_THRESHOLD);
        LocalTime noonThreshold = parseTimeConfig("noon_threshold", MedicationCalcUtil.DEFAULT_NOON_THRESHOLD);
        LocalTime eveningThreshold = parseTimeConfig("evening_threshold", MedicationCalcUtil.DEFAULT_EVENING_THRESHOLD);

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime lastCalcTime = stock.getLastCalcTime();

        // 如果lastCalcTime为空，初始化为当前时间并跳过扣减
        if (lastCalcTime == null) {
            stock.setLastCalcTime(now);
            stock.setTodayDeductedPeriods("[]");
            stock.setLastDeductionDate(now.toLocalDate());
            stock.setRemainingDays(MedicationCalcUtil.calcRemainingDays(
                    stock.getTotalRemainingUnits(), prescription.getDailyConsumption()));
            updateById(stock);
            return null;
        }

        // 调用核心算法计算扣减
        MedicationCalcUtil.PeriodDeductResult result = MedicationCalcUtil.calcStockOnLoginWithPeriod(
                stock.getTotalRemainingUnits(),
                prescription.getTakeFrequencyCode(),
                prescription.getDosagePerTime(),
                prescription.getDailyConsumption(),
                lastCalcTime,
                now,
                stock.getTodayDeductedPeriods(),
                stock.getLastDeductionDate(),
                morningThreshold,
                noonThreshold,
                eveningThreshold
        );

        // 如果有扣减，更新库存
        if (result.getDeductedUnits() > 0) {
            stock.setTotalRemainingUnits(result.getRemainingUnits());
            stock.setRemainingDays(result.getRemainingDays());
            stock.setTodayDeductedPeriods(MedicationCalcUtil.toJsonArray(result.getTodayDeductedPeriods()));
            stock.setLastDeductionDate(now.toLocalDate());
            stock.setLastCalcTime(now);

            // 乐观锁更新：version字段由MyBatis-Plus @Version自动处理
            boolean updated = updateById(stock);
            if (!updated) {
                // 乐观锁冲突，重试一次
                stock = getById(stockId);
                if (stock == null) return null;
                // 重新计算
                result = MedicationCalcUtil.calcStockOnLoginWithPeriod(
                        stock.getTotalRemainingUnits(),
                        prescription.getTakeFrequencyCode(),
                        prescription.getDosagePerTime(),
                        prescription.getDailyConsumption(),
                        stock.getLastCalcTime(),
                        now,
                        stock.getTodayDeductedPeriods(),
                        stock.getLastDeductionDate(),
                        morningThreshold, noonThreshold, eveningThreshold
                );
                stock.setTotalRemainingUnits(result.getRemainingUnits());
                stock.setRemainingDays(result.getRemainingDays());
                stock.setTodayDeductedPeriods(MedicationCalcUtil.toJsonArray(result.getTodayDeductedPeriods()));
                stock.setLastDeductionDate(now.toLocalDate());
                stock.setLastCalcTime(now);
                updated = updateById(stock);
                if (!updated) {
                    log.warn("乐观锁冲突重试仍失败: stockId={}", stockId);
                    throw new BusinessException("库存更新冲突，请稍后重试");
                }
            }

            // 记录扣减日志
            logDeduction(stockId, prescription, result);
        } else {
            // 即使没有扣减，也更新lastCalcTime保证时间推进
            stock.setLastCalcTime(now);
            // 跨天时重置todayDeductedPeriods
            if (stock.getLastDeductionDate() == null || !stock.getLastDeductionDate().equals(now.toLocalDate())) {
                stock.setTodayDeductedPeriods("[]");
                stock.setLastDeductionDate(now.toLocalDate());
            }
            stock.setTodayDeductedPeriods(MedicationCalcUtil.toJsonArray(result.getTodayDeductedPeriods()));
            updateById(stock);
        }

        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deductAllByUserIdWithPeriod(Long userId) {
        LambdaQueryWrapper<Prescription> pWrapper = new LambdaQueryWrapper<>();
        pWrapper.eq(Prescription::getUserId, userId).eq(Prescription::getStatus, 1);
        List<Prescription> prescriptions = prescriptionMapper.selectList(pWrapper);
        for (Prescription p : prescriptions) {
            Stock stock = getByPrescriptionId(p.getPrescriptionId());
            if (stock != null) {
                deductOnLoginWithPeriod(stock.getStockId());
            }
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deductAllWithPeriod() {
        List<Stock> stocks = list();
        for (Stock stock : stocks) {
            deductOnLoginWithPeriod(stock.getStockId());
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void manualAdjustStock(Long stockId, int adjustUnits, Long operatorId, String reason) {
        Stock stock = getById(stockId);
        if (stock == null) {
            throw new BusinessException("库存记录不存在");
        }
        int beforeUnits = stock.getTotalRemainingUnits();
        int newUnits = beforeUnits + adjustUnits;
        // 库存最低为0
        if (newUnits < 0) {
            newUnits = 0;
        }
        stock.setTotalRemainingUnits(newUnits);

        Prescription prescription = prescriptionMapper.selectById(stock.getPrescriptionId());
        if (prescription != null && prescription.getDailyConsumption() > 0) {
            stock.setRemainingDays(MedicationCalcUtil.calcRemainingDays(newUnits, prescription.getDailyConsumption()));
        }
        stock.setLastCalcTime(LocalDateTime.now());
        boolean updated = updateById(stock);
        if (!updated) {
            throw new BusinessException("库存更新冲突，请稍后重试");
        }

        // 记录调整日志
        String logContent = String.format("手动修正库存: 库存编号=%d, 调整量=%d, 修正前=%d, 修正后=%d, 原因=%s",
                stockId, adjustUnits, beforeUnits, newUnits, reason);
        sysLogService.log(operatorId, "手动修正库存", logContent, null);
        log.info(logContent);
    }

    /**
     * 根据处方ID获取库存记录
     */
    private Stock getByPrescriptionId(Long prescriptionId) {
        LambdaQueryWrapper<Stock> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Stock::getPrescriptionId, prescriptionId);
        return getOne(wrapper);
    }

    /**
     * 解析时间配置
     */
    private LocalTime parseTimeConfig(String configKey, LocalTime defaultValue) {
        try {
            String value = sysConfigService.getConfigValue(configKey);
            if (value != null && !value.isEmpty()) {
                return LocalTime.parse(value);
            }
        } catch (Exception e) {
            log.warn("解析配置{}失败，使用默认值{}", configKey, defaultValue);
        }
        return defaultValue;
    }

    /**
     * 记录扣减日志
     */
    private void logDeduction(Long stockId, Prescription prescription, MedicationCalcUtil.PeriodDeductResult result) {
        String logContent = String.format(
                "分时段扣减: 库存编号=%d, 扣减时段=%s, 扣减量=%d, 扣减前=%d, 扣减后=%d, 剩余可吃天数=%d",
                stockId,
                result.getDeductedPeriods(),
                result.getDeductedUnits(),
                result.getOriginalUnits(),
                result.getRemainingUnits(),
                result.getRemainingDays()
        );
        sysLogService.log(0L, "分时段扣减", logContent, null);
        log.info(logContent);
    }
}

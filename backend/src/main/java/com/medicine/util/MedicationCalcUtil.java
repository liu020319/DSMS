package com.medicine.util;

import cn.hutool.json.JSONUtil;
import com.medicine.common.TakeFrequencyEnum;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.*;

/**
 * 药品库存计算工具类
 * 封装所有与药品消耗、库存扣减相关的算法
 * 算法可独立调用，方便其他地方传参使用
 */
public class MedicationCalcUtil {

    /**
     * 时段常量定义
     */
    public static final String PERIOD_MORNING = "MORNING";
    public static final String PERIOD_NOON = "NOON";
    public static final String PERIOD_EVENING = "EVENING";

    /**
     * 默认时段阈值时间
     */
    public static final LocalTime DEFAULT_MORNING_THRESHOLD = LocalTime.of(9, 0);
    public static final LocalTime DEFAULT_NOON_THRESHOLD = LocalTime.of(13, 0);
    public static final LocalTime DEFAULT_EVENING_THRESHOLD = LocalTime.of(21, 0);

    /**
     * 原有方法：计算剩余可吃天数
     * 保留100%不变，兼容已有调用
     */
    public static int calcRemainingDays(int totalRemainingUnits, int dailyConsumption) {
        if (dailyConsumption <= 0) return 0;
        return totalRemainingUnits / dailyConsumption;
    }

    /**
     * 原有方法：计算购买盒数（向上取整，保证买的量>=所需天数）
     * 保留100%不变，兼容已有调用
     */
    public static int calcBoxCount(int days, int dailyConsumption, int unitPerBox) {
        if (unitPerBox <= 0 || dailyConsumption <= 0) return 0;
        int neededUnits = days * dailyConsumption;
        return (int) Math.ceil((double) neededUnits / unitPerBox);
    }

    /**
     * 原有方法：计算单盒可吃天数
     * 保留100%不变，兼容已有调用
     */
    public static int calcDaysPerBox(int unitPerBox, int dailyConsumption) {
        if (dailyConsumption <= 0) return 0;
        return unitPerBox / dailyConsumption;
    }

    /**
     * ============================================================
     * 新增方法1：calcPeriodDeductDosage
     * 分时段单次扣减量匹配算法
     * 根据服用频次和时段，计算某个时段应该扣减的剂量
     * ============================================================
     *
     * @param takeFrequencyCode 服用频次枚举值
     * @param dosagePerTime     每次用量
     * @param period            要扣减的时段 MORNING/NOON/EVENING
     * @return 该时段应扣减的剂量，如果该时段不需要服药则返回0
     */
    public static int calcPeriodDeductDosage(String takeFrequencyCode, int dosagePerTime, String period) {
        if (takeFrequencyCode == null || period == null || dosagePerTime <= 0) {
            return 0;
        }
        TakeFrequencyEnum frequency = TakeFrequencyEnum.fromCode(takeFrequencyCode);
        List<String> periods = frequency.getPeriods();
        // 只有该频次包含的时段才扣减，其他时段返回0
        if (periods.contains(period)) {
            return dosagePerTime;
        }
        return 0;
    }

    /**
     * ============================================================
     * 新增方法2：calcStockOnLoginWithPeriod
     * 登录时分时段全量库存扣减核心算法
     * 严格按步骤执行：先批量扣历史完整天数，再扣当天分时段剂量
     * ============================================================
     *
     * 算法步骤：
     * 1. 判断lastCalcTime与now的关系
     * 2. 如果是同一天：只扣当天已过时段的剂量（排除已扣减的时段）
     * 3. 如果是不同天：
     *    a. 扣除lastCalcTime当天剩余时段的剂量
     *    b. 扣除中间完整天的每日消耗量
     *    c. 扣除当天已过时段的剂量
     * 4. 库存最低为0，不允许负库存
     * 5. 更新todayDeductedPeriods记录当天已扣减的时段
     *
     * @param totalRemainingUnits 当前剩余总单位数
     * @param takeFrequencyCode   服用频次枚举值
     * @param dosagePerTime       每次用量
     * @param dailyConsumption    每日消耗量
     * @param lastCalcTime        上次计算时间
     * @param now                 当前时间
     * @param todayDeductedPeriods 当日已扣减时段JSON字符串
     * @param lastDeductionDate   上次扣减日期
     * @param morningThreshold    晨服阈值时间
     * @param noonThreshold       午服阈值时间
     * @param eveningThreshold    晚服阈值时间
     * @return 扣减结果对象
     */
    public static PeriodDeductResult calcStockOnLoginWithPeriod(
            int totalRemainingUnits,
            String takeFrequencyCode,
            int dosagePerTime,
            int dailyConsumption,
            LocalDateTime lastCalcTime,
            LocalDateTime now,
            String todayDeductedPeriods,
            LocalDate lastDeductionDate,
            LocalTime morningThreshold,
            LocalTime noonThreshold,
            LocalTime eveningThreshold) {

        PeriodDeductResult result = new PeriodDeductResult();
        result.setOriginalUnits(totalRemainingUnits);

        // 参数校验
        if (totalRemainingUnits <= 0 || dailyConsumption <= 0 || takeFrequencyCode == null) {
            result.setRemainingUnits(Math.max(totalRemainingUnits, 0));
            result.setDeductedUnits(0);
            result.setDeductedPeriods(new ArrayList<>());
            result.setTodayDeductedPeriods(parseJsonArray(todayDeductedPeriods));
            result.setRemainingDays(calcRemainingDays(Math.max(totalRemainingUnits, 0), dailyConsumption));
            return result;
        }

        TakeFrequencyEnum frequency = TakeFrequencyEnum.fromCode(takeFrequencyCode);
        List<String> allPeriods = frequency.getPeriods();

        // 解析当日已扣减时段
        List<String> deductedToday = parseJsonArray(todayDeductedPeriods);

        // 判断是否需要重置当日已扣减记录（跨天了）
        LocalDate today = now.toLocalDate();
        if (lastDeductionDate == null || !lastDeductionDate.equals(today)) {
            deductedToday = new ArrayList<>();
        }

        int totalDeducted = 0;
        List<String> newDeductedPeriods = new ArrayList<>();

        LocalDate lastDate = lastCalcTime.toLocalDate();
        LocalTime lastTime = lastCalcTime.toLocalTime();

        if (lastDate.equals(today)) {
            // 同一天：只扣当天lastTime之后、nowTime之前已过时段的剂量
            List<String> passedPeriods = getPassedPeriods(now, allPeriods, morningThreshold, noonThreshold, eveningThreshold);
            for (String period : passedPeriods) {
                // 幂等校验：已扣减的时段不再扣减
                if (deductedToday.contains(period)) {
                    continue;
                }
                int deduct = calcPeriodDeductDosage(takeFrequencyCode, dosagePerTime, period);
                totalDeducted += deduct;
                newDeductedPeriods.add(period);
                deductedToday.add(period);
            }
        } else {
            // 不同天：分3步扣减

            // 步骤a：扣除lastCalcTime当天剩余时段的剂量
            List<String> lastDayRemainingPeriods = getRemainingPeriodsAfterTime(lastTime, allPeriods, morningThreshold, noonThreshold, eveningThreshold);
            for (String period : lastDayRemainingPeriods) {
                int deduct = calcPeriodDeductDosage(takeFrequencyCode, dosagePerTime, period);
                totalDeducted += deduct;
            }

            // 步骤b：扣除中间完整天的每日消耗量
            long fullDaysBetween = ChronoUnit.DAYS.between(lastDate, today) - 1;
            if (fullDaysBetween > 0) {
                totalDeducted += (int) fullDaysBetween * dailyConsumption;
            }

            // 步骤c：扣除当天已过时段的剂量
            List<String> todayPassedPeriods = getPassedPeriods(now, allPeriods, morningThreshold, noonThreshold, eveningThreshold);
            for (String period : todayPassedPeriods) {
                // 当天新扣减，幂等校验
                if (deductedToday.contains(period)) {
                    continue;
                }
                int deduct = calcPeriodDeductDosage(takeFrequencyCode, dosagePerTime, period);
                totalDeducted += deduct;
                newDeductedPeriods.add(period);
                deductedToday.add(period);
            }
        }

        // 库存最低为0，不允许负库存
        int newRemaining = totalRemainingUnits - totalDeducted;
        if (newRemaining < 0) {
            newRemaining = 0;
        }
        int actualDeducted = totalRemainingUnits - newRemaining;

        result.setRemainingUnits(newRemaining);
        result.setDeductedUnits(actualDeducted);
        result.setDeductedPeriods(newDeductedPeriods);
        result.setTodayDeductedPeriods(deductedToday);
        result.setRemainingDays(calcRemainingDays(newRemaining, dailyConsumption));

        return result;
    }

    /**
     * ============================================================
     * 新增方法3：checkPeriodDeductIdempotent
     * 幂等性校验算法
     * 生成唯一幂等流水号，校验是否已扣减，防止重复扣减
     * ============================================================
     *
     * 幂等流水号格式：{prescriptionId}_{日期}_{时段}
     * 例如：1_2026-04-24_MORNING
     * 同一天同一时段的流水号唯一，已存在则表示已扣减
     *
     * @param prescriptionId 方案ID
     * @param date           扣减日期
     * @param period         扣减时段
     * @param existingDeductedPeriods 当日已扣减时段列表
     * @return true=已扣减(重复)，false=未扣减(可扣减)
     */
    public static boolean checkPeriodDeductIdempotent(Long prescriptionId, LocalDate date, String period, List<String> existingDeductedPeriods) {
        String idempotentKey = generateIdempotentKey(prescriptionId, date, period);
        // 检查当日已扣减时段中是否包含该时段
        if (existingDeductedPeriods != null && existingDeductedPeriods.contains(period)) {
            return true;
        }
        return false;
    }

    /**
     * 生成幂等流水号
     * 格式：{prescriptionId}_{date}_{period}
     */
    public static String generateIdempotentKey(Long prescriptionId, LocalDate date, String period) {
        return prescriptionId + "_" + date.toString() + "_" + period;
    }

    /**
     * 获取当前时间已过的时段列表
     * 根据当前时间和阈值时间判断哪些时段已过
     */
    public static List<String> getPassedPeriods(LocalDateTime now, List<String> allPeriods,
                                                 LocalTime morningThreshold, LocalTime noonThreshold, LocalTime eveningThreshold) {
        List<String> passed = new ArrayList<>();
        LocalTime currentTime = now.toLocalTime();
        for (String period : allPeriods) {
            LocalTime threshold = getThresholdForPeriod(period, morningThreshold, noonThreshold, eveningThreshold);
            if (threshold != null && !currentTime.isBefore(threshold)) {
                passed.add(period);
            }
        }
        return passed;
    }

    /**
     * 获取指定时间之后剩余的时段列表
     * 用于计算lastCalcTime当天剩余未扣减的时段
     */
    public static List<String> getRemainingPeriodsAfterTime(LocalTime time, List<String> allPeriods,
                                                             LocalTime morningThreshold, LocalTime noonThreshold, LocalTime eveningThreshold) {
        List<String> remaining = new ArrayList<>();
        for (String period : allPeriods) {
            LocalTime threshold = getThresholdForPeriod(period, morningThreshold, noonThreshold, eveningThreshold);
            if (threshold != null && time.isBefore(threshold)) {
                remaining.add(period);
            }
        }
        return remaining;
    }

    /**
     * 根据时段获取对应的阈值时间
     */
    public static LocalTime getThresholdForPeriod(String period, LocalTime morningThreshold, LocalTime noonThreshold, LocalTime eveningThreshold) {
        switch (period) {
            case PERIOD_MORNING: return morningThreshold != null ? morningThreshold : DEFAULT_MORNING_THRESHOLD;
            case PERIOD_NOON: return noonThreshold != null ? noonThreshold : DEFAULT_NOON_THRESHOLD;
            case PERIOD_EVENING: return eveningThreshold != null ? eveningThreshold : DEFAULT_EVENING_THRESHOLD;
            default: return null;
        }
    }

    /**
     * 解析JSON数组字符串为List
     */
    public static List<String> parseJsonArray(String json) {
        if (json == null || json.trim().isEmpty() || "[]".equals(json.trim())) {
            return new ArrayList<>();
        }
        try {
            return JSONUtil.toList(json, String.class);
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }

    /**
     * List转JSON字符串
     */
    public static String toJsonArray(List<String> list) {
        if (list == null || list.isEmpty()) {
            return "[]";
        }
        return JSONUtil.toJsonStr(list);
    }

    /**
     * 分时段扣减结果对象
     */
    public static class PeriodDeductResult {
        private int originalUnits;
        private int deductedUnits;
        private int remainingUnits;
        private int remainingDays;
        private List<String> deductedPeriods;
        private List<String> todayDeductedPeriods;

        public int getOriginalUnits() { return originalUnits; }
        public void setOriginalUnits(int originalUnits) { this.originalUnits = originalUnits; }
        public int getDeductedUnits() { return deductedUnits; }
        public void setDeductedUnits(int deductedUnits) { this.deductedUnits = deductedUnits; }
        public int getRemainingUnits() { return remainingUnits; }
        public void setRemainingUnits(int remainingUnits) { this.remainingUnits = remainingUnits; }
        public int getRemainingDays() { return remainingDays; }
        public void setRemainingDays(int remainingDays) { this.remainingDays = remainingDays; }
        public List<String> getDeductedPeriods() { return deductedPeriods; }
        public void setDeductedPeriods(List<String> deductedPeriods) { this.deductedPeriods = deductedPeriods; }
        public List<String> getTodayDeductedPeriods() { return todayDeductedPeriods; }
        public void setTodayDeductedPeriods(List<String> todayDeductedPeriods) { this.todayDeductedPeriods = todayDeductedPeriods; }
    }
}

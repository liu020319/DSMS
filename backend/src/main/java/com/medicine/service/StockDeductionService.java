package com.medicine.service;

import com.medicine.util.MedicationCalcUtil;

/**
 * 库存扣减事务服务接口
 * 所有扣减操作使用乐观锁+事务保证原子性
 */
public interface StockDeductionService {

    /**
     * 按当前时间补齐尚未执行的分时段库存扣减（核心方法）
     * 使用乐观锁保证并发安全，扣减SQL加version版本号校验
     *
     * @param stockId 库存ID
     * @return 扣减结果
     */
    MedicationCalcUtil.PeriodDeductResult deductOnLoginWithPeriod(Long stockId);

    /**
     * 批量扣减指定用户的所有在用药品库存
     *
     * @param userId 用户ID
     */
    void deductAllByUserIdWithPeriod(Long userId);

    /**
     * 批量扣减所有药品库存
     */
    void deductAllWithPeriod();

    /**
     * 手动修正库存（补扣/回滚）
     *
     * @param stockId       库存ID
     * @param adjustUnits   调整单位数（正数补扣，负数回滚）
     * @param operatorId    操作人ID
     * @param reason        调整原因
     */
    void manualAdjustStock(Long stockId, int adjustUnits, Long operatorId, String reason);
}

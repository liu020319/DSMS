package com.medicine.task;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.medicine.entity.Stock;
import com.medicine.mapper.StockMapper;
import com.medicine.util.MedicationCalcUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;

/**
 * 库存定时任务
 * 每日凌晨0点自动重置所有药品的today_deducted_periods为空数组
 * 更新last_deduction_date为当前日期
 */
@Slf4j
@Component
public class StockDailyResetTask {

    @Autowired
    private StockMapper stockMapper;

    /**
     * 每日凌晨0点执行
     * 重置所有库存记录的当日已扣减时段
     */
    @Scheduled(cron = "0 0 0 * * ?")
    public void resetDailyDeductedPeriods() {
        log.info("=== 开始执行每日库存扣减时段重置任务 ===");
        LambdaQueryWrapper<Stock> wrapper = new LambdaQueryWrapper<>();
        List<Stock> stocks = stockMapper.selectList(wrapper);
        int count = 0;
        for (Stock stock : stocks) {
            stock.setTodayDeductedPeriods("[]");
            stock.setLastDeductionDate(LocalDate.now());
            stockMapper.updateById(stock);
            count++;
        }
        log.info("=== 每日库存扣减时段重置任务完成，共重置{}条记录 ===", count);
    }
}

package com.medicine.task;

import com.medicine.service.StockDeductionService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 服务器常驻时每分钟检查一次应执行的早、中、晚用药扣减。
 * 核心算法会记录当天已经扣减的时段，因此重复检查不会重复扣减。
 */
@Slf4j
@Component
public class StockPeriodDeductionTask {

    @Autowired
    private StockDeductionService stockDeductionService;

    @Scheduled(cron = "0 * * * * ?")
    public void deductDuePeriods() {
        try {
            stockDeductionService.deductAllWithPeriod();
        } catch (Exception e) {
            log.error("定时库存扣减检查失败", e);
        }
    }
}

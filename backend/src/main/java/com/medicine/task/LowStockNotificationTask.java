package com.medicine.task;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.medicine.entity.SysUser;
import com.medicine.entity.UserNotification;
import com.medicine.mapper.SysUserMapper;
import com.medicine.mapper.UserNotificationMapper;
import com.medicine.service.NotificationService;
import com.medicine.service.StockService;
import com.medicine.vo.StockVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Component
public class LowStockNotificationTask {
    @Autowired private SysUserMapper userMapper;
    @Autowired private UserNotificationMapper notificationMapper;
    @Autowired private StockService stockService;
    @Autowired private NotificationService notificationService;

    @Scheduled(cron = "0 0 9 * * ?")
    public void notifyCaregivers() {
        List<SysUser> elders = userMapper.selectList(new LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getRole, "ELDER").eq(SysUser::getStatus, 1));
        for (SysUser elder : elders) {
            if (elder.getBindParentId() == null) continue;
            List<StockVO> warning = stockService.getWarningList(elder.getUserId());
            if (warning.isEmpty() || alreadySentToday(elder.getBindParentId(), elder.getUserId())) continue;
            String names = warning.stream().limit(5).map(x -> x.getMedicineName() + "(剩" + x.getRemainingDays() + "天)").collect(Collectors.joining("、"));
            notificationService.notify(elder.getBindParentId(), elder.getRealName() + "有药品快用完", names + "。请尽快查看并安排购药。", "LOW_STOCK", elder.getUserId());
        }
    }

    private boolean alreadySentToday(Long recipientId, Long elderId) {
        return notificationMapper.selectCount(new LambdaQueryWrapper<UserNotification>()
                .eq(UserNotification::getRecipientId, recipientId)
                .eq(UserNotification::getBizType, "LOW_STOCK")
                .eq(UserNotification::getBizId, elderId)
                .ge(UserNotification::getCreateTime, LocalDate.now().atStartOfDay())) > 0;
    }
}

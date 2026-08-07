package com.medicine.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.medicine.entity.SysLog;
import com.medicine.mapper.SysLogMapper;
import com.medicine.service.SysLogService;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class SysLogServiceImpl extends ServiceImpl<SysLogMapper, SysLog> implements SysLogService {

    @Override
    public void log(Long userId, String operationType, String operationContent, String operationIp) {
        SysLog sysLog = new SysLog();
        sysLog.setUserId(userId);
        sysLog.setOperationType(operationType);
        sysLog.setOperationContent(operationContent);
        sysLog.setOperationIp(operationIp);
        sysLog.setOperationTime(LocalDateTime.now());
        save(sysLog);
    }

    @Override
    public Page<SysLog> pageList(int current, int size, Long userId, String operationType) {
        Page<SysLog> page = new Page<>(current, size);
        LambdaQueryWrapper<SysLog> wrapper = new LambdaQueryWrapper<>();
        if (userId != null) {
            wrapper.eq(SysLog::getUserId, userId);
        }
        if (operationType != null && !operationType.isEmpty()) {
            wrapper.eq(SysLog::getOperationType, operationType);
        }
        wrapper.orderByDesc(SysLog::getOperationTime);
        return page(page, wrapper);
    }
}

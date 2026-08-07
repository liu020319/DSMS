package com.medicine.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.medicine.entity.SysLog;

public interface SysLogService extends IService<SysLog> {
    void log(Long userId, String operationType, String operationContent, String operationIp);
    com.baomidou.mybatisplus.extension.plugins.pagination.Page<SysLog> pageList(int current, int size, Long userId, String operationType);
}

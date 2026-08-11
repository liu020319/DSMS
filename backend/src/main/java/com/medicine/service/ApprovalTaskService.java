package com.medicine.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.medicine.entity.ApprovalTask;

public interface ApprovalTaskService extends IService<ApprovalTask> {
    void submitTask(com.medicine.dto.ApprovalTaskDTO dto);
    void approveTask(Long taskId, String comment, Long handlerId);
    void rejectTask(Long taskId, String comment, Long handlerId);
    void modifyAndApproveTask(Long taskId, com.medicine.dto.ApprovalTaskDTO modifiedDto, String comment, Long handlerId);
    com.baomidou.mybatisplus.extension.plugins.pagination.Page<ApprovalTask> pagePending(int current, int size, Long handlerId);
    com.baomidou.mybatisplus.extension.plugins.pagination.Page<ApprovalTask> pageByApplicant(int current, int size, Long applicantId);
    com.baomidou.mybatisplus.extension.plugins.pagination.Page<ApprovalTask> pageAll(int current, int size, Long handlerId);
}

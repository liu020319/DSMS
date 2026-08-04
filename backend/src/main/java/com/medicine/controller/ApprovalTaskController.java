package com.medicine.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.medicine.common.Result;
import com.medicine.dto.ApprovalTaskDTO;
import com.medicine.entity.ApprovalTask;
import com.medicine.service.ApprovalTaskService;
import com.medicine.service.SysLogService;
import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/approval")
public class ApprovalTaskController {

    @Autowired
    private ApprovalTaskService approvalTaskService;

    @Autowired
    private SysLogService sysLogService;

    @PostMapping("/submit")
    public Result<Void> submit(@Valid @RequestBody ApprovalTaskDTO dto, HttpServletRequest request) {
        approvalTaskService.submitTask(dto);
        sysLogService.log(getUserId(request), "SUBMIT_APPROVAL", "提交审批申请: " + dto.getTaskType(), request.getRemoteAddr());
        return Result.success();
    }

    @PutMapping("/approve/{id}")
    public Result<Void> approve(@PathVariable("id") Long id,
                                @RequestParam(required = false) String comment,
                                HttpServletRequest request) {
        approvalTaskService.approveTask(id, comment, getUserId(request));
        sysLogService.log(getUserId(request), "APPROVE_TASK", "审批通过ID: " + id, request.getRemoteAddr());
        return Result.success();
    }

    @PutMapping("/reject/{id}")
    public Result<Void> reject(@PathVariable("id") Long id,
                               @RequestParam(required = false) String comment,
                               HttpServletRequest request) {
        approvalTaskService.rejectTask(id, comment, getUserId(request));
        sysLogService.log(getUserId(request), "REJECT_TASK", "审批驳回ID: " + id, request.getRemoteAddr());
        return Result.success();
    }

    @PutMapping("/modify-approve/{id}")
    public Result<Void> modifyAndApprove(@PathVariable("id") Long id,
                                         @RequestBody ApprovalTaskDTO modifiedDto,
                                         @RequestParam(required = false) String comment,
                                         HttpServletRequest request) {
        approvalTaskService.modifyAndApproveTask(id, modifiedDto, comment, getUserId(request));
        sysLogService.log(getUserId(request), "MODIFY_APPROVE_TASK", "修改并审批通过ID: " + id, request.getRemoteAddr());
        return Result.success();
    }

    @GetMapping("/pending")
    public Result<Page<ApprovalTask>> pending(
            @RequestParam(defaultValue = "1") int current,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) Long handlerId) {
        return Result.success(approvalTaskService.pagePending(current, size, handlerId));
    }

    @GetMapping("/my")
    public Result<Page<ApprovalTask>> myTasks(
            @RequestParam(defaultValue = "1") int current,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam Long applicantId) {
        return Result.success(approvalTaskService.pageByApplicant(current, size, applicantId));
    }

    @GetMapping("/all")
    public Result<Page<ApprovalTask>> all(
            @RequestParam(defaultValue = "1") int current,
            @RequestParam(defaultValue = "10") int size) {
        return Result.success(approvalTaskService.pageAll(current, size));
    }

    @GetMapping("/{id}")
    public Result<ApprovalTask> getById(@PathVariable("id") Long id) {
        return Result.success(approvalTaskService.getById(id));
    }

    private Long getUserId(HttpServletRequest request) {
        return (Long) request.getAttribute("userId");
    }
}

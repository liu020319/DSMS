package com.medicine.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.medicine.common.Result;
import com.medicine.entity.SysLog;
import com.medicine.service.SysLogService;
import com.medicine.util.AccessControl;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/log")
public class SysLogController {

    @Autowired
    private SysLogService sysLogService;

    @Autowired
    private AccessControl accessControl;

    @GetMapping("/page")
    public Result<Page<SysLog>> page(
            @RequestParam(defaultValue = "1") int current,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) String operationType,
            HttpServletRequest request) {
        accessControl.requireAdmin(request);
        return Result.success(sysLogService.pageList(current, size, scopedLogUserId(request, userId), operationType));
    }

    @GetMapping("/export")
    public void export(@RequestParam(required = false) Long userId,
                       @RequestParam(required = false) String operationType,
                       HttpServletResponse response,
                       HttpServletRequest request) throws Exception {
        accessControl.requireAdmin(request);
        userId = scopedLogUserId(request, userId);
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setHeader("Content-Disposition", "attachment;filename=sys_log_export.xlsx");
        Page<SysLog> page = sysLogService.pageList(1, 10000, userId, operationType);
        com.alibaba.excel.EasyExcel.write(response.getOutputStream(), SysLog.class)
                .sheet("操作日志").doWrite(page.getRecords());
    }

    private Long scopedLogUserId(HttpServletRequest request, Long requestedUserId) {
        if (accessControl.isSystemAdmin(request)) return requestedUserId;
        Long currentUserId = accessControl.userId(request);
        if (requestedUserId == null || currentUserId.equals(requestedUserId)) return currentUserId;
        accessControl.requireOwnerOrAdmin(request, requestedUserId);
        return requestedUserId;
    }
}

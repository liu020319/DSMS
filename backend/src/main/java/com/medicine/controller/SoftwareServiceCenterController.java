package com.medicine.controller;

import com.medicine.common.Result;
import com.medicine.dto.SoftwareMilestoneDTO;
import com.medicine.dto.SoftwareServiceRequestDTO;
import com.medicine.dto.SoftwareServiceStatusDTO;
import com.medicine.dto.SoftwareWorkOrderDTO;
import com.medicine.dto.SoftwareWorkOrderStatusDTO;
import com.medicine.entity.SoftwareServiceMilestone;
import com.medicine.entity.SoftwareServiceRequest;
import com.medicine.entity.SoftwareServiceWorkOrder;
import com.medicine.service.SoftwareServiceCenterService;
import com.medicine.service.SysLogService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/portal/services")
public class SoftwareServiceCenterController {
    private final SoftwareServiceCenterService serviceCenter;
    private final SysLogService sysLogService;

    public SoftwareServiceCenterController(SoftwareServiceCenterService serviceCenter,
                                           SysLogService sysLogService) {
        this.serviceCenter = serviceCenter;
        this.sysLogService = sysLogService;
    }

    @GetMapping("/requests")
    public Result<List<SoftwareServiceRequest>> requests(@RequestParam(required = false) String status,
                                                          HttpServletRequest request) {
        return Result.success(serviceCenter.listRequests(userId(request), role(request), status));
    }

    @GetMapping("/requests/{id}")
    public Result<Map<String, Object>> detail(@PathVariable("id") Long id, HttpServletRequest request) {
        return Result.success(serviceCenter.detail(id, userId(request), role(request)));
    }

    @PostMapping("/requests")
    public Result<SoftwareServiceRequest> createRequest(@Valid @RequestBody SoftwareServiceRequestDTO dto,
                                                         HttpServletRequest request) {
        SoftwareServiceRequest created = serviceCenter.createRequest(userId(request), dto);
        log(request, "提交软件服务需求", "需求ID：" + created.getRequestId());
        return Result.success(created);
    }

    @PutMapping("/requests/{id}/status")
    public Result<SoftwareServiceRequest> updateStatus(@PathVariable("id") Long id,
                                                        @Valid @RequestBody SoftwareServiceStatusDTO dto,
                                                        HttpServletRequest request) {
        SoftwareServiceRequest updated = serviceCenter.updateStatus(id, dto, role(request));
        log(request, "处理软件服务需求", "需求ID：" + id + "，状态：" + updated.getStatus());
        return Result.success(updated);
    }

    @PutMapping("/requests/{id}/cancel")
    public Result<SoftwareServiceRequest> cancel(@PathVariable("id") Long id, HttpServletRequest request) {
        SoftwareServiceRequest updated = serviceCenter.cancelRequest(id, userId(request), role(request));
        log(request, "取消软件服务需求", "需求ID：" + id);
        return Result.success(updated);
    }

    @PostMapping("/requests/{id}/milestones")
    public Result<SoftwareServiceMilestone> createMilestone(@PathVariable("id") Long id,
                                                             @Valid @RequestBody SoftwareMilestoneDTO dto,
                                                             HttpServletRequest request) {
        SoftwareServiceMilestone milestone = serviceCenter.saveMilestone(id, null, dto, role(request));
        log(request, "新增软件服务里程碑", "需求ID：" + id);
        return Result.success(milestone);
    }

    @PutMapping("/requests/{requestId}/milestones/{milestoneId}")
    public Result<SoftwareServiceMilestone> updateMilestone(@PathVariable Long requestId,
                                                             @PathVariable Long milestoneId,
                                                             @Valid @RequestBody SoftwareMilestoneDTO dto,
                                                             HttpServletRequest request) {
        SoftwareServiceMilestone milestone = serviceCenter.saveMilestone(requestId, milestoneId, dto, role(request));
        log(request, "更新软件服务里程碑", "里程碑ID：" + milestoneId);
        return Result.success(milestone);
    }

    @GetMapping("/work-orders")
    public Result<List<SoftwareServiceWorkOrder>> workOrders(@RequestParam(required = false) String status,
                                                              HttpServletRequest request) {
        return Result.success(serviceCenter.listWorkOrders(userId(request), role(request), status));
    }

    @PostMapping("/work-orders")
    public Result<SoftwareServiceWorkOrder> createWorkOrder(@Valid @RequestBody SoftwareWorkOrderDTO dto,
                                                             HttpServletRequest request) {
        SoftwareServiceWorkOrder created = serviceCenter.createWorkOrder(userId(request), role(request), dto);
        log(request, "提交软件服务工单", "工单ID：" + created.getWorkOrderId());
        return Result.success(created);
    }

    @PutMapping("/work-orders/{id}/status")
    public Result<SoftwareServiceWorkOrder> updateWorkOrder(@PathVariable("id") Long id,
                                                             @Valid @RequestBody SoftwareWorkOrderStatusDTO dto,
                                                             HttpServletRequest request) {
        SoftwareServiceWorkOrder updated = serviceCenter.updateWorkOrder(id, dto, userId(request), role(request));
        log(request, "处理软件服务工单", "工单ID：" + id + "，状态：" + updated.getStatus());
        return Result.success(updated);
    }

    private Long userId(HttpServletRequest request) {
        return (Long) request.getAttribute("userId");
    }

    private String role(HttpServletRequest request) {
        return String.valueOf(request.getAttribute("role"));
    }

    private void log(HttpServletRequest request, String type, String content) {
        sysLogService.log(userId(request), type, content, request.getRemoteAddr());
    }
}

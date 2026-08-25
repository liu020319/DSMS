package com.medicine.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.medicine.common.Result;
import com.medicine.dto.PurchaseRecordDTO;
import com.medicine.dto.PurchaseStatsFilter;
import com.medicine.entity.PurchaseRecord;
import com.medicine.service.PurchaseRecordService;
import com.medicine.service.SysLogService;
import com.medicine.util.AccessControl;
import com.medicine.vo.PurchaseRecordVO;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/purchase")
public class PurchaseRecordController {

    @Autowired
    private PurchaseRecordService purchaseRecordService;

    @Autowired
    private SysLogService sysLogService;

    @Autowired
    private AccessControl accessControl;

    @PostMapping("/add")
    public Result<Void> add(@Valid @RequestBody PurchaseRecordDTO dto, HttpServletRequest request) {
        accessControl.requireAdmin(request);
        accessControl.requireOwnerOrAdmin(request, dto.getUserId());
        dto.setOperatorId(getUserId(request));
        purchaseRecordService.addPurchaseRecord(dto);
        sysLogService.log(getUserId(request), "新增购药记录", "新增购药记录", request.getRemoteAddr());
        return Result.success();
    }

    @PutMapping("/update")
    public Result<Void> update(@Valid @RequestBody PurchaseRecordDTO dto, HttpServletRequest request) {
        accessControl.requireAdmin(request);
        PurchaseRecord existing = purchaseRecordService.getById(dto.getPurchaseId());
        if (existing != null) accessControl.requireOwnerOrAdmin(request, existing.getUserId());
        accessControl.requireOwnerOrAdmin(request, dto.getUserId());
        purchaseRecordService.updatePurchaseRecord(dto);
        sysLogService.log(getUserId(request), "修改购药记录", "修改购药记录，编号: " + dto.getPurchaseId(), request.getRemoteAddr());
        return Result.success();
    }

    @DeleteMapping("/delete/{id}")
    public Result<Void> delete(@PathVariable("id") Long id, HttpServletRequest request) {
        accessControl.requireAdmin(request);
        PurchaseRecord existing = purchaseRecordService.getById(id);
        if (existing != null) accessControl.requireOwnerOrAdmin(request, existing.getUserId());
        purchaseRecordService.deletePurchaseRecord(id);
        sysLogService.log(getUserId(request), "删除购药记录", "删除购药记录，编号: " + id, request.getRemoteAddr());
        return Result.success();
    }

    @PutMapping("/confirm-receipt/{id}")
    public Result<Void> confirmReceipt(@PathVariable("id") Long id, HttpServletRequest request) {
        accessControl.requireAdmin(request);
        PurchaseRecord existing = purchaseRecordService.getById(id);
        if (existing != null) accessControl.requireOwnerOrAdmin(request, existing.getUserId());
        purchaseRecordService.confirmReceipt(id);
        sysLogService.log(getUserId(request), "确认收货", "确认收货，购药记录编号: " + id, request.getRemoteAddr());
        return Result.success();
    }

    @GetMapping("/page")
    public Result<Page<PurchaseRecordVO>> page(
            @RequestParam(defaultValue = "1") int current,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) Long prescriptionId,
            @RequestParam(required = false) String approvalNumber,
            HttpServletRequest request) {
        accessControl.requireAdmin(request);
        return Result.success(purchaseRecordService.pageList(current, size, userId, prescriptionId, approvalNumber,
                accessControl.scopedUserIds(request, userId)));
    }

    @GetMapping("/stats/monthly")
    public Result<List<Map<String, Object>>> monthlyStats(@ModelAttribute PurchaseStatsFilter filter, HttpServletRequest request) {
        accessControl.requireAdmin(request);
        return Result.success(purchaseRecordService.getMonthlyStats(filter, accessControl.scopedUserIds(request, filter.getUserId())));
    }

    @GetMapping("/stats/daily")
    public Result<List<Map<String, Object>>> dailyStats(
            @ModelAttribute PurchaseStatsFilter filter,
            HttpServletRequest request) {
        accessControl.requireAdmin(request);
        return Result.success(purchaseRecordService.getDailyStats(filter, accessControl.scopedUserIds(request, filter.getUserId())));
    }

    @GetMapping("/stats/yearly")
    public Result<List<Map<String, Object>>> yearlyStats(@ModelAttribute PurchaseStatsFilter filter, HttpServletRequest request) {
        accessControl.requireAdmin(request);
        return Result.success(purchaseRecordService.getYearlyStats(filter, accessControl.scopedUserIds(request, filter.getUserId())));
    }

    @GetMapping("/stats/weekly")
    public Result<List<Map<String, Object>>> weeklyStats(@ModelAttribute PurchaseStatsFilter filter, HttpServletRequest request) {
        accessControl.requireAdmin(request); return Result.success(purchaseRecordService.getWeeklyStats(filter, accessControl.scopedUserIds(request, filter.getUserId())));
    }
    @GetMapping("/stats/platform")
    public Result<List<Map<String, Object>>> platformStats(@ModelAttribute PurchaseStatsFilter filter, HttpServletRequest request) {
        accessControl.requireAdmin(request); return Result.success(purchaseRecordService.getPlatformStats(filter, accessControl.scopedUserIds(request, filter.getUserId())));
    }
    @GetMapping("/stats/channel")
    public Result<List<Map<String, Object>>> channelStats(@ModelAttribute PurchaseStatsFilter filter, HttpServletRequest request) {
        accessControl.requireAdmin(request); return Result.success(purchaseRecordService.getChannelStats(filter, accessControl.scopedUserIds(request, filter.getUserId())));
    }
    @GetMapping("/stats/time-bucket")
    public Result<List<Map<String, Object>>> timeBucketStats(@ModelAttribute PurchaseStatsFilter filter, HttpServletRequest request) {
        accessControl.requireAdmin(request); return Result.success(purchaseRecordService.getTimeBucketStats(filter, accessControl.scopedUserIds(request, filter.getUserId())));
    }
    @GetMapping("/stats/summary")
    public Result<Map<String, Object>> expenseSummary(@ModelAttribute PurchaseStatsFilter filter, HttpServletRequest request) {
        accessControl.requireAdmin(request); return Result.success(purchaseRecordService.getExpenseSummary(filter, accessControl.scopedUserIds(request, filter.getUserId())));
    }

    @GetMapping("/export")
    public void export(@RequestParam(required = false) Long userId, HttpServletResponse response, HttpServletRequest request) throws Exception {
        accessControl.requireAdmin(request);
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setHeader("Content-Disposition", "attachment;filename=purchase_export.xlsx");
        List<PurchaseRecordVO> list = purchaseRecordService.listForExport(userId, accessControl.scopedUserIds(request, userId));
        com.alibaba.excel.EasyExcel.write(response.getOutputStream(), PurchaseRecordVO.class)
                .sheet("购药记录").doWrite(list);
    }

    private Long getUserId(HttpServletRequest request) {
        return (Long) request.getAttribute("userId");
    }
}

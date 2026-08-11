package com.medicine.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.medicine.common.Result;
import com.medicine.entity.Medicine;
import com.medicine.entity.PurchaseRecord;
import com.medicine.entity.SysLog;
import com.medicine.entity.SysUser;
import com.medicine.service.*;
import com.medicine.util.AccessControl;
import javax.servlet.http.HttpServletRequest;
import com.medicine.vo.*;
import javax.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/export")
public class ExportController {

    @Autowired
    private MedicineService medicineService;

    @Autowired
    private PrescriptionService prescriptionService;

    @Autowired
    private PurchaseRecordService purchaseRecordService;

    @Autowired
    private SysUserService sysUserService;

    @Autowired
    private SysLogService sysLogService;

    @Autowired
    private AccessControl accessControl;

    @GetMapping("/medicine")
    public void exportMedicine(HttpServletResponse response, HttpServletRequest request) throws Exception {
        accessControl.requireAdmin(request);
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setHeader("Content-Disposition", "attachment;filename=" + java.net.URLEncoder.encode("药品档案", "UTF-8") + ".xlsx");
        List<Medicine> list = medicineService.list();
        List<MedicineExportVO> exportList = new ArrayList<>();
        for (Medicine m : list) {
            MedicineExportVO vo = new MedicineExportVO();
            vo.setMedicineId(m.getMedicineId());
            vo.setApprovalNumber(m.getApprovalNumber());
            vo.setMedicineName(m.getMedicineName());
            vo.setBrandName(m.getBrandName());
            vo.setSpecification(m.getSpecification());
            vo.setUnitPerBox(m.getUnitPerBox());
            vo.setManufacturer(m.getManufacturer());
            vo.setReferencePrice(m.getReferencePrice());
            vo.setStatusText(m.getStatus() == 1 ? "启用" : "禁用");
            exportList.add(vo);
        }
        com.alibaba.excel.EasyExcel.write(response.getOutputStream(), MedicineExportVO.class)
                .sheet("药品档案").doWrite(exportList);
    }

    @GetMapping("/user")
    public void exportUser(HttpServletResponse response, HttpServletRequest request) throws Exception {
        accessControl.requireAdmin(request);
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setHeader("Content-Disposition", "attachment;filename=" + java.net.URLEncoder.encode("用户数据", "UTF-8") + ".xlsx");
        List<SysUser> list = accessControl.isSystemAdmin(request)
                ? sysUserService.list()
                : sysUserService.getFamilyUsers((Long) request.getAttribute("userId"));
        List<UserExportVO> exportList = new ArrayList<>();
        for (SysUser u : list) {
            UserExportVO vo = new UserExportVO();
            vo.setUserId(u.getUserId());
            vo.setUsername(u.getUsername());
            vo.setRealName(u.getRealName());
            vo.setPhone(u.getPhone());
            vo.setRoleText("ADMIN".equals(u.getRole()) ? "平台管理员" : "GUARDIAN".equals(u.getRole()) ? "家庭守护人" : "安心用药成员");
            vo.setStatusText(u.getStatus() == 1 ? "启用" : "禁用");
            exportList.add(vo);
        }
        com.alibaba.excel.EasyExcel.write(response.getOutputStream(), UserExportVO.class)
                .sheet("用户数据").doWrite(exportList);
    }

    @GetMapping("/log")
    public void exportLog(HttpServletResponse response, HttpServletRequest request) throws Exception {
        accessControl.requireAdmin(request);
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setHeader("Content-Disposition", "attachment;filename=" + java.net.URLEncoder.encode("操作日志", "UTF-8") + ".xlsx");
        Long auditUserId = accessControl.isSystemAdmin(request) ? null : (Long) request.getAttribute("userId");
        Page<SysLog> page = sysLogService.pageList(1, 10000, auditUserId, null);
        List<LogExportVO> exportList = new ArrayList<>();
        for (SysLog l : page.getRecords()) {
            LogExportVO vo = new LogExportVO();
            vo.setLogId(l.getLogId());
            vo.setUserId(l.getUserId());
            vo.setOperationType(l.getOperationType());
            vo.setOperationContent(l.getOperationContent());
            vo.setOperationIp(l.getOperationIp());
            vo.setOperationTime(l.getOperationTime() != null ? l.getOperationTime().toString() : "");
            exportList.add(vo);
        }
        com.alibaba.excel.EasyExcel.write(response.getOutputStream(), LogExportVO.class)
                .sheet("操作日志").doWrite(exportList);
    }

    @GetMapping("/purchase")
    public void exportPurchase(@RequestParam(required = false) Long userId, HttpServletResponse response, HttpServletRequest request) throws Exception {
        accessControl.requireAdmin(request);
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setHeader("Content-Disposition", "attachment;filename=" + java.net.URLEncoder.encode("购药记录", "UTF-8") + ".xlsx");
        List<Long> allowedUserIds = accessControl.scopedUserIds(request, userId);
        List<PurchaseRecordVO> list = purchaseRecordService.listForExport(userId, allowedUserIds);
        List<PurchaseExportVO> exportList = new ArrayList<>();
        for (PurchaseRecordVO r : list) {
            PurchaseExportVO vo = new PurchaseExportVO();
            vo.setPurchaseId(r.getPurchaseId());
            vo.setUserName(r.getUserName());
            vo.setMedicineName(r.getMedicineName());
            vo.setApprovalNumber(r.getApprovalNumber());
            vo.setBrandName(r.getBrandName());
            vo.setSpecification(r.getSpecification());
            vo.setPurchaseDate(r.getPurchaseDate() != null ? r.getPurchaseDate().toString() : "");
            vo.setPurchaseTime(r.getPurchaseTime() != null ? r.getPurchaseTime().toString().replace('T', ' ') : "");
            vo.setQuantityBoxes(r.getQuantityBoxes());
            vo.setUnitPrice(r.getUnitPrice());
            vo.setTotalPrice(r.getTotalPrice());
            vo.setExpiryDate(r.getExpiryDate() != null ? r.getExpiryDate().toString() : "");
            vo.setPurchasePlatform(r.getPurchasePlatform());
            vo.setPurchaseChannel("OFFLINE".equals(r.getPurchaseChannel()) ? "线下" : "线上");
            vo.setOrderId(r.getOrderId());
            vo.setProofUrl(r.getProofUrl());
            vo.setReceiptStatus(Integer.valueOf(1).equals(r.getReceiptStatus()) ? "已确认收货" : "已下单未收货");
            vo.setOperatorName(r.getOperatorName());
            exportList.add(vo);
        }
        com.alibaba.excel.ExcelWriter writer = com.alibaba.excel.EasyExcel.write(response.getOutputStream()).build();
        try {
            writer.write(exportList, com.alibaba.excel.EasyExcel.writerSheet(0, "购药明细").head(PurchaseExportVO.class).build());
            writer.write(toSummary(purchaseRecordService.getYearlyStats(userId, allowedUserIds), "year"), com.alibaba.excel.EasyExcel.writerSheet(1, "年度汇总").head(ExpenseSummaryExportVO.class).build());
            writer.write(toSummary(purchaseRecordService.getMonthlyStats(userId, allowedUserIds), "month"), com.alibaba.excel.EasyExcel.writerSheet(2, "月度汇总").head(ExpenseSummaryExportVO.class).build());
            writer.write(toSummary(purchaseRecordService.getWeeklyStats(userId, allowedUserIds), "week"), com.alibaba.excel.EasyExcel.writerSheet(3, "周度汇总").head(ExpenseSummaryExportVO.class).build());
            writer.write(toSummary(purchaseRecordService.getPlatformStats(userId, allowedUserIds), "name"), com.alibaba.excel.EasyExcel.writerSheet(4, "平台汇总").head(ExpenseSummaryExportVO.class).build());
            writer.write(toSummary(purchaseRecordService.getDailyStats(userId, java.time.LocalDate.now().minusYears(10).toString(), allowedUserIds), "day"), com.alibaba.excel.EasyExcel.writerSheet(5, "日度汇总").head(ExpenseSummaryExportVO.class).build());
            writer.write(toSummary(purchaseRecordService.getChannelStats(userId, allowedUserIds), "name"), com.alibaba.excel.EasyExcel.writerSheet(6, "线上线下汇总").head(ExpenseSummaryExportVO.class).build());
            writer.write(toSummary(purchaseRecordService.getTimeBucketStats(userId, allowedUserIds), "name"), com.alibaba.excel.EasyExcel.writerSheet(7, "购药时段汇总").head(ExpenseSummaryExportVO.class).build());
        } finally {
            writer.finish();
        }
    }

    private List<ExpenseSummaryExportVO> toSummary(List<Map<String, Object>> source, String dimensionKey) {
        List<ExpenseSummaryExportVO> result = new ArrayList<>();
        for (Map<String, Object> row : source) {
            ExpenseSummaryExportVO item = new ExpenseSummaryExportVO();
            item.setDimension(String.valueOf(row.get(dimensionKey)));
            Object amount = row.get("total_amount");
            item.setTotalAmount(amount == null ? java.math.BigDecimal.ZERO : new java.math.BigDecimal(String.valueOf(amount)));
            Object count = row.get("count");
            item.setPurchaseCount(count == null ? 0L : Long.valueOf(String.valueOf(count)));
            Object average = row.get("average_amount");
            item.setAverageAmount(average == null ? java.math.BigDecimal.ZERO : new java.math.BigDecimal(String.valueOf(average)));
            result.add(item);
        }
        return result;
    }
}

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
        List<SysUser> list = sysUserService.list();
        List<UserExportVO> exportList = new ArrayList<>();
        for (SysUser u : list) {
            UserExportVO vo = new UserExportVO();
            vo.setUserId(u.getUserId());
            vo.setUsername(u.getUsername());
            vo.setRealName(u.getRealName());
            vo.setPhone(u.getPhone());
            vo.setRoleText("ADMIN".equals(u.getRole()) ? "子女" : "老人");
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
        Page<SysLog> page = sysLogService.pageList(1, 10000, null, null);
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
        List<PurchaseRecordVO> list = purchaseRecordService.listForExport(userId);
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
            vo.setQuantityBoxes(r.getQuantityBoxes());
            vo.setUnitPrice(r.getUnitPrice());
            vo.setTotalPrice(r.getTotalPrice());
            vo.setExpiryDate(r.getExpiryDate() != null ? r.getExpiryDate().toString() : "");
            vo.setPurchasePlatform(r.getPurchasePlatform());
            vo.setOperatorName(r.getOperatorName());
            exportList.add(vo);
        }
        com.alibaba.excel.EasyExcel.write(response.getOutputStream(), PurchaseExportVO.class)
                .sheet("购药记录").doWrite(exportList);
    }
}

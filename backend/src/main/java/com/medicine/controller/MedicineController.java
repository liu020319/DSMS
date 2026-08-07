package com.medicine.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.medicine.common.Result;
import com.medicine.dto.MedicineDTO;
import com.medicine.entity.Medicine;
import com.medicine.service.MedicineService;
import com.medicine.service.SysLogService;
import com.medicine.util.AccessControl;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/medicine")
public class MedicineController {

    @Autowired
    private MedicineService medicineService;

    @Autowired
    private SysLogService sysLogService;

    @Autowired
    private AccessControl accessControl;

    @PostMapping("/add")
    public Result<Void> add(@Valid @RequestBody MedicineDTO dto, HttpServletRequest request) {
        accessControl.requireAdmin(request);
        medicineService.addMedicine(dto);
        sysLogService.log(getUserId(request), "新增药品", "新增药品: " + dto.getMedicineName(), request.getRemoteAddr());
        return Result.success();
    }

    @PutMapping("/update")
    public Result<Void> update(@Valid @RequestBody MedicineDTO dto, HttpServletRequest request) {
        accessControl.requireAdmin(request);
        medicineService.updateMedicine(dto);
        sysLogService.log(getUserId(request), "修改药品", "修改药品: " + dto.getMedicineName(), request.getRemoteAddr());
        return Result.success();
    }

    @PutMapping("/disable/{id}")
    public Result<Void> disable(@PathVariable("id") Long id, HttpServletRequest request) {
        accessControl.requireAdmin(request);
        medicineService.disableMedicine(id);
        sysLogService.log(getUserId(request), "禁用启用药品", "禁用/启用药品，编号: " + id, request.getRemoteAddr());
        return Result.success();
    }

    @DeleteMapping("/delete/{id}")
    public Result<Void> delete(@PathVariable("id") Long id, HttpServletRequest request) {
        accessControl.requireAdmin(request);
        medicineService.deleteMedicine(id);
        sysLogService.log(getUserId(request), "删除药品", "删除药品，编号: " + id, request.getRemoteAddr());
        return Result.success();
    }

    @GetMapping("/page")
    public Result<Page<Medicine>> page(
            @RequestParam(defaultValue = "1") int current,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String approvalNumber,
            HttpServletRequest request) {
        accessControl.requireAdmin(request);
        return Result.success(medicineService.pageList(current, size, keyword, approvalNumber));
    }

    @GetMapping("/list")
    public Result<List<Medicine>> list() {
        return Result.success(medicineService.listActive());
    }

    @GetMapping("/{id}")
    public Result<Medicine> getById(@PathVariable("id") Long id) {
        return Result.success(medicineService.getById(id));
    }

    @GetMapping("/export")
    public void export(HttpServletResponse response, HttpServletRequest request) throws Exception {
        accessControl.requireAdmin(request);
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setHeader("Content-Disposition", "attachment;filename=medicine_export.xlsx");
        List<Medicine> list = medicineService.list();
        com.alibaba.excel.EasyExcel.write(response.getOutputStream(), Medicine.class)
                .sheet("药品档案").doWrite(list);
    }

    private Long getUserId(HttpServletRequest request) {
        return (Long) request.getAttribute("userId");
    }
}

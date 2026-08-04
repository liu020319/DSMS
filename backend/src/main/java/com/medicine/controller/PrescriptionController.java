package com.medicine.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.medicine.common.Result;
import com.medicine.dto.PrescriptionDTO;
import com.medicine.service.PrescriptionService;
import com.medicine.service.SysLogService;
import com.medicine.util.AccessControl;
import com.medicine.vo.PrescriptionVO;
import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/prescription")
public class PrescriptionController {

    @Autowired
    private PrescriptionService prescriptionService;

    @Autowired
    private SysLogService sysLogService;

    @Autowired
    private AccessControl accessControl;

    @PostMapping("/add")
    public Result<Void> add(@Valid @RequestBody PrescriptionDTO dto, HttpServletRequest request) {
        accessControl.requireAdmin(request);
        prescriptionService.addPrescription(dto);
        sysLogService.log(getUserId(request), "新增用药方案", "新增用药方案", request.getRemoteAddr());
        return Result.success();
    }

    @PutMapping("/update")
    public Result<Void> update(@Valid @RequestBody PrescriptionDTO dto,
                               @RequestParam(required = false) String changeReason,
                               HttpServletRequest request) {
        accessControl.requireAdmin(request);
        prescriptionService.updatePrescription(dto, changeReason);
        sysLogService.log(getUserId(request), "修改用药方案", "修改用药方案，编号: " + dto.getPrescriptionId(), request.getRemoteAddr());
        return Result.success();
    }

    @PutMapping("/stop/{id}")
    public Result<Void> stop(@PathVariable("id") Long id, HttpServletRequest request) {
        accessControl.requireAdmin(request);
        prescriptionService.stopPrescription(id);
        sysLogService.log(getUserId(request), "停用用药方案", "停用用药方案，编号: " + id, request.getRemoteAddr());
        return Result.success();
    }

    @PutMapping("/enable/{id}")
    public Result<Void> enable(@PathVariable("id") Long id, HttpServletRequest request) {
        accessControl.requireAdmin(request);
        prescriptionService.enablePrescription(id);
        sysLogService.log(getUserId(request), "启用用药方案", "启用用药方案，编号: " + id, request.getRemoteAddr());
        return Result.success();
    }

    @GetMapping("/page")
    public Result<Page<PrescriptionVO>> page(
            @RequestParam(defaultValue = "1") int current,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) Long medicineId,
            @RequestParam(required = false) String realName,
            HttpServletRequest request) {
        accessControl.requireAdmin(request);
        return Result.success(prescriptionService.pageList(current, size, userId, medicineId, realName));
    }

    @GetMapping("/list/{userId}")
    public Result<List<PrescriptionVO>> listByUserId(@PathVariable("userId") Long userId, HttpServletRequest request) {
        accessControl.requireOwnerOrAdmin(request, userId);
        return Result.success(prescriptionService.listByUserId(userId));
    }

    @GetMapping("/detail/{id}")
    public Result<PrescriptionVO> detail(@PathVariable("id") Long id, HttpServletRequest request) {
        accessControl.requireAdmin(request);
        return Result.success(prescriptionService.getDetail(id));
    }

    @GetMapping("/history/{id}")
    public Result<List<PrescriptionVO>> history(@PathVariable("id") Long id, HttpServletRequest request) {
        accessControl.requireAdmin(request);
        return Result.success(prescriptionService.getHistory(id));
    }

    private Long getUserId(HttpServletRequest request) {
        return (Long) request.getAttribute("userId");
    }
}

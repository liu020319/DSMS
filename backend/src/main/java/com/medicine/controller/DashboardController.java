package com.medicine.controller;

import com.medicine.common.Result;
import com.medicine.service.DashboardService;
import com.medicine.service.StockDeductionService;
import com.medicine.service.StockService;
import com.medicine.util.AccessControl;
import com.medicine.vo.DashboardVO;
import com.medicine.vo.StockVO;
import javax.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/dashboard")
public class DashboardController {

    @Autowired
    private DashboardService dashboardService;

    @Autowired
    private StockService stockService;

    @Autowired
    private StockDeductionService stockDeductionService;

    @Autowired
    private com.medicine.service.PrescriptionService prescriptionService;

    @Autowired
    private AccessControl accessControl;

    @GetMapping("/admin")
    public Result<DashboardVO> adminDashboard(HttpServletRequest request) {
        accessControl.requireAdmin(request);
        java.util.List<Long> scoped = accessControl.scopedUserIds(request, null);
        return Result.success(dashboardService.getAdminDashboard(scoped,
                accessControl.isSystemAdmin(request) ? null : accessControl.userId(request)));
    }

    @GetMapping("/elder")
    public Result<DashboardVO> elderDashboard(@RequestParam Long userId, HttpServletRequest request) {
        accessControl.requireOwnerOrAdmin(request, userId);
        return Result.success(dashboardService.getElderDashboard(userId));
    }

    @GetMapping("/stock/all")
    public Result<List<StockVO>> allStock(@RequestParam(required = false) Long userId, HttpServletRequest request) {
        if ("ELDER".equals(request.getAttribute("role"))) {
            accessControl.requireOwnerOrAdmin(request, userId);
            return Result.success(stockService.getAllStockDetail(userId));
        }
        accessControl.requireAdmin(request);
        return Result.success(scopedStocks(request, userId, "all"));
    }

    @GetMapping("/stock/warning")
    public Result<List<StockVO>> warningStock(@RequestParam(required = false) Long userId, HttpServletRequest request) {
        accessControl.requireAdmin(request);
        return Result.success(scopedStocks(request, userId, "warning"));
    }

    @GetMapping("/stock/expiring")
    public Result<List<StockVO>> expiringStock(@RequestParam(required = false) Long userId, HttpServletRequest request) {
        accessControl.requireAdmin(request);
        return Result.success(scopedStocks(request, userId, "expiring"));
    }

    @GetMapping("/stock/calc-boxes")
    public Result<Integer> calcBoxes(@RequestParam Long prescriptionId, @RequestParam Integer days, HttpServletRequest request) {
        accessControl.requireAdmin(request);
        com.medicine.vo.PrescriptionVO prescription = prescriptionService.getDetail(prescriptionId);
        if (prescription != null) accessControl.requireOwnerOrAdmin(request, prescription.getUserId());
        return Result.success(stockService.calculateBoxCount(prescriptionId, days));
    }

    /**
     * 库存手动修正接口（补扣/回滚）
     */
    @PostMapping("/stock/manual-adjust")
    public Result<Void> manualAdjustStock(@RequestParam Long stockId,
                                          @RequestParam Integer adjustUnits,
                                          @RequestParam(required = false) String reason,
                                          HttpServletRequest request) {
        accessControl.requireAdmin(request);
        com.medicine.entity.Stock stock = stockService.getById(stockId);
        if (stock != null) {
            com.medicine.vo.PrescriptionVO prescription = prescriptionService.getDetail(stock.getPrescriptionId());
            if (prescription != null) accessControl.requireOwnerOrAdmin(request, prescription.getUserId());
        }
        Long operatorId = (Long) request.getAttribute("userId");
        stockDeductionService.manualAdjustStock(stockId, adjustUnits, operatorId, reason);
        return Result.success();
    }

    private List<StockVO> scopedStocks(HttpServletRequest request, Long requestedUserId, String type) {
        java.util.List<Long> scoped = accessControl.scopedUserIds(request, requestedUserId);
        if (scoped == null) return stockByType(requestedUserId, type);
        java.util.List<StockVO> result = new java.util.ArrayList<>();
        for (Long userId : scoped) result.addAll(stockByType(userId, type));
        return result;
    }

    private List<StockVO> stockByType(Long userId, String type) {
        if ("warning".equals(type)) return stockService.getWarningList(userId);
        if ("expiring".equals(type)) return stockService.getExpiringList(userId);
        return stockService.getAllStockDetail(userId);
    }
}

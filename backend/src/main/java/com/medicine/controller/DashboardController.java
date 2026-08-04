package com.medicine.controller;

import com.medicine.common.Result;
import com.medicine.service.DashboardService;
import com.medicine.service.StockDeductionService;
import com.medicine.service.StockService;
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

    @GetMapping("/admin")
    public Result<DashboardVO> adminDashboard() {
        return Result.success(dashboardService.getAdminDashboard());
    }

    @GetMapping("/elder")
    public Result<DashboardVO> elderDashboard(@RequestParam Long userId) {
        return Result.success(dashboardService.getElderDashboard(userId));
    }

    @GetMapping("/stock/all")
    public Result<List<StockVO>> allStock(@RequestParam(required = false) Long userId) {
        return Result.success(stockService.getAllStockDetail(userId));
    }

    @GetMapping("/stock/warning")
    public Result<List<StockVO>> warningStock(@RequestParam(required = false) Long userId) {
        return Result.success(stockService.getWarningList(userId));
    }

    @GetMapping("/stock/expiring")
    public Result<List<StockVO>> expiringStock(@RequestParam(required = false) Long userId) {
        return Result.success(stockService.getExpiringList(userId));
    }

    @GetMapping("/stock/calc-boxes")
    public Result<Integer> calcBoxes(@RequestParam Long prescriptionId, @RequestParam Integer days) {
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
        Long operatorId = (Long) request.getAttribute("userId");
        stockDeductionService.manualAdjustStock(stockId, adjustUnits, operatorId, reason);
        return Result.success();
    }
}

package com.medicine.vo;

import lombok.Data;

@Data
public class DashboardVO {
    private Long totalMedicines;
    private Long activePrescriptions;
    private Long warningCount;
    private Long pendingApprovalCount;
    private Long expiringCount;
    private java.util.List<StockVO> warningList;
    private java.util.List<StockVO> expiringList;
}

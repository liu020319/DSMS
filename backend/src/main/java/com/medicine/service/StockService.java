package com.medicine.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.medicine.entity.Stock;
import com.medicine.vo.StockVO;
import java.util.List;

public interface StockService extends IService<Stock> {
    void calculateRemainingDays(Long stockId);
    void calculateAllByUserId(Long userId);
    void calculateAll();
    void addStockOnPurchase(Long prescriptionId, Integer quantityBoxes, java.time.LocalDate expiryDate);
    void deductStockOnLoss(Long prescriptionId, Integer lossBoxes);
    Stock getByPrescriptionId(Long prescriptionId);
    List<StockVO> getWarningList(Long userId);
    List<StockVO> getExpiringList(Long userId);
    List<StockVO> getAllStockDetail(Long userId);
    int calculateBoxCount(Long prescriptionId, Integer days);
}

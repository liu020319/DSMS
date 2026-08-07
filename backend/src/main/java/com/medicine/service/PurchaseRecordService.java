package com.medicine.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.medicine.entity.PurchaseRecord;
import com.medicine.vo.PurchaseRecordVO;
import java.util.List;
import java.util.Map;

public interface PurchaseRecordService extends IService<PurchaseRecord> {
    void addPurchaseRecord(com.medicine.dto.PurchaseRecordDTO dto);
    void updatePurchaseRecord(com.medicine.dto.PurchaseRecordDTO dto);
    void deletePurchaseRecord(Long purchaseId);
    void confirmReceipt(Long purchaseId);
    void confirmFamilyReceipt(Long purchaseId);
    com.baomidou.mybatisplus.extension.plugins.pagination.Page<PurchaseRecordVO> pageList(int current, int size, Long userId, Long prescriptionId, String approvalNumber);
    List<Map<String, Object>> getMonthlyStats(Long userId);
    List<Map<String, Object>> getDailyStats(Long userId, String startDate);
    List<Map<String, Object>> getYearlyStats(Long userId);
    List<Map<String, Object>> getWeeklyStats(Long userId);
    List<Map<String, Object>> getPlatformStats(Long userId);
    List<Map<String, Object>> getChannelStats(Long userId);
    List<Map<String, Object>> getTimeBucketStats(Long userId);
    Map<String, Object> getExpenseSummary(Long userId);
    List<PurchaseRecordVO> listForExport(Long userId);
}

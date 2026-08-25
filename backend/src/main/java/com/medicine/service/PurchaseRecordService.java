package com.medicine.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.medicine.entity.PurchaseRecord;
import com.medicine.vo.PurchaseRecordVO;
import java.util.List;
import java.util.Map;
import com.medicine.dto.PurchaseStatsFilter;

public interface PurchaseRecordService extends IService<PurchaseRecord> {
    void addPurchaseRecord(com.medicine.dto.PurchaseRecordDTO dto);
    void updatePurchaseRecord(com.medicine.dto.PurchaseRecordDTO dto);
    void deletePurchaseRecord(Long purchaseId);
    void confirmReceipt(Long purchaseId);
    void confirmFamilyReceipt(Long purchaseId);
    com.baomidou.mybatisplus.extension.plugins.pagination.Page<PurchaseRecordVO> pageList(int current, int size, Long userId, Long prescriptionId, String approvalNumber, List<Long> allowedUserIds);
    List<Map<String, Object>> getMonthlyStats(PurchaseStatsFilter filter, List<Long> allowedUserIds);
    List<Map<String, Object>> getDailyStats(PurchaseStatsFilter filter, List<Long> allowedUserIds);
    List<Map<String, Object>> getYearlyStats(PurchaseStatsFilter filter, List<Long> allowedUserIds);
    List<Map<String, Object>> getWeeklyStats(PurchaseStatsFilter filter, List<Long> allowedUserIds);
    List<Map<String, Object>> getPlatformStats(PurchaseStatsFilter filter, List<Long> allowedUserIds);
    List<Map<String, Object>> getChannelStats(PurchaseStatsFilter filter, List<Long> allowedUserIds);
    List<Map<String, Object>> getTimeBucketStats(PurchaseStatsFilter filter, List<Long> allowedUserIds);
    Map<String, Object> getExpenseSummary(PurchaseStatsFilter filter, List<Long> allowedUserIds);
    List<PurchaseRecordVO> listForExport(Long userId, List<Long> allowedUserIds);
}

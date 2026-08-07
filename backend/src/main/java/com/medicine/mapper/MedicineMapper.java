package com.medicine.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.medicine.entity.Medicine;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

@Mapper
public interface MedicineMapper extends BaseMapper<Medicine> {

    @Select("SELECT COUNT(*) AS total_count, " +
            "SUM(CASE WHEN status = 1 THEN 1 ELSE 0 END) AS active_count, " +
            "SUM(CASE WHEN status = 0 THEN 1 ELSE 0 END) AS disabled_count, " +
            "ROUND(COALESCE(AVG(reference_price), 0), 2) AS average_price, " +
            "SUM(CASE WHEN image_url IS NULL OR image_url = '' OR reference_price IS NULL " +
            "OR manufacturer IS NULL OR manufacturer = '' THEN 1 ELSE 0 END) AS incomplete_count " +
            "FROM medicine WHERE deleted = 0")
    Map<String, Object> selectMedicineSummary();

    @Select("SELECT " +
            "(SELECT COUNT(*) FROM prescription p WHERE p.medicine_id = #{medicineId} AND p.deleted = 0) AS prescription_count, " +
            "(SELECT COUNT(*) FROM prescription p WHERE p.medicine_id = #{medicineId} AND p.deleted = 0 AND p.status = 1) AS active_prescription_count, " +
            "(SELECT COUNT(*) FROM stock s JOIN prescription p ON s.prescription_id = p.prescription_id " +
            " WHERE p.medicine_id = #{medicineId} AND s.deleted = 0 AND p.deleted = 0 AND s.remaining_days < 7) AS low_stock_count, " +
            "(SELECT COUNT(*) FROM purchase_record pr JOIN prescription p ON pr.prescription_id = p.prescription_id " +
            " WHERE p.medicine_id = #{medicineId} AND pr.deleted = 0 AND p.deleted = 0) AS purchase_count, " +
            "(SELECT COALESCE(SUM(pr.quantity_boxes), 0) FROM purchase_record pr JOIN prescription p ON pr.prescription_id = p.prescription_id " +
            " WHERE p.medicine_id = #{medicineId} AND pr.deleted = 0 AND p.deleted = 0) AS total_purchase_boxes, " +
            "(SELECT COALESCE(SUM(pr.total_price), 0) FROM purchase_record pr JOIN prescription p ON pr.prescription_id = p.prescription_id " +
            " WHERE p.medicine_id = #{medicineId} AND pr.deleted = 0 AND p.deleted = 0) AS total_purchase_amount, " +
            "(SELECT MAX(COALESCE(pr.purchase_time, pr.create_time)) FROM purchase_record pr JOIN prescription p ON pr.prescription_id = p.prescription_id " +
            " WHERE p.medicine_id = #{medicineId} AND pr.deleted = 0 AND p.deleted = 0) AS latest_purchase_time")
    Map<String, Object> selectMedicineProfileStats(Long medicineId);

    @Select("SELECT pr.purchase_id, pr.purchase_date, pr.purchase_time, pr.quantity_boxes, pr.unit_price, " +
            "pr.total_price, pr.purchase_platform, pr.purchase_channel, pr.receipt_status, u.real_name AS user_name " +
            "FROM purchase_record pr " +
            "JOIN prescription p ON pr.prescription_id = p.prescription_id AND p.deleted = 0 " +
            "LEFT JOIN sys_user u ON pr.user_id = u.user_id AND u.deleted = 0 " +
            "WHERE p.medicine_id = #{medicineId} AND pr.deleted = 0 " +
            "ORDER BY COALESCE(pr.purchase_time, pr.create_time) DESC LIMIT 8")
    List<Map<String, Object>> selectRecentPurchases(Long medicineId);

    @Select("SELECT u.user_id, u.real_name, p.prescription_id, p.daily_times, p.dosage_per_time, " +
            "p.dosage_unit, p.take_timing, p.status, s.remaining_days, s.total_remaining_units, s.expiry_date " +
            "FROM prescription p " +
            "JOIN sys_user u ON p.user_id = u.user_id AND u.deleted = 0 " +
            "LEFT JOIN stock s ON s.prescription_id = p.prescription_id AND s.deleted = 0 " +
            "WHERE p.medicine_id = #{medicineId} AND p.deleted = 0 " +
            "ORDER BY p.status DESC, s.remaining_days ASC LIMIT 8")
    List<Map<String, Object>> selectRelatedUsers(Long medicineId);
}

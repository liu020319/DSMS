package com.medicine.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.medicine.entity.PurchaseRecord;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Param;
import java.util.List;
import java.util.Map;

@Mapper
public interface PurchaseRecordMapper extends BaseMapper<PurchaseRecord> {

    @Select("SELECT pr.*, m.medicine_name, m.approval_number, m.brand_name, m.specification, " +
            "u.real_name as user_name, op.real_name as operator_name " +
            "FROM purchase_record pr " +
            "LEFT JOIN prescription p ON pr.prescription_id = p.prescription_id AND p.deleted = 0 " +
            "LEFT JOIN medicine m ON p.medicine_id = m.medicine_id AND m.deleted = 0 " +
            "LEFT JOIN sys_user u ON pr.user_id = u.user_id AND u.deleted = 0 " +
            "LEFT JOIN sys_user op ON pr.operator_id = op.user_id AND op.deleted = 0 " +
            "WHERE pr.deleted = 0 ORDER BY pr.purchase_date DESC")
    List<Map<String, Object>> selectPurchaseWithDetail();

    List<Map<String, Object>> selectMonthlyStatsDynamic(@Param("userId") Long userId, @Param("allowedUserIds") List<Long> allowedUserIds);
    List<Map<String, Object>> selectDailyStatsDynamic(@Param("userId") Long userId, @Param("startDate") String startDate, @Param("allowedUserIds") List<Long> allowedUserIds);
    List<Map<String, Object>> selectYearlyStatsDynamic(@Param("userId") Long userId, @Param("allowedUserIds") List<Long> allowedUserIds);
    List<Map<String, Object>> selectWeeklyStatsDynamic(@Param("userId") Long userId, @Param("allowedUserIds") List<Long> allowedUserIds);
    List<Map<String, Object>> selectPlatformStatsDynamic(@Param("userId") Long userId, @Param("allowedUserIds") List<Long> allowedUserIds);
    List<Map<String, Object>> selectChannelStatsDynamic(@Param("userId") Long userId, @Param("allowedUserIds") List<Long> allowedUserIds);
    List<Map<String, Object>> selectTimeBucketStatsDynamic(@Param("userId") Long userId, @Param("allowedUserIds") List<Long> allowedUserIds);
    Map<String, Object> selectExpenseSummary(@Param("userId") Long userId, @Param("allowedUserIds") List<Long> allowedUserIds);
}

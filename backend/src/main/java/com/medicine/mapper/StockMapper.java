package com.medicine.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.medicine.entity.Stock;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import java.util.List;
import java.util.Map;

@Mapper
public interface StockMapper extends BaseMapper<Stock> {

    @Select("SELECT s.*, p.user_id, p.medicine_id, p.daily_consumption, " +
            "p.take_frequency_code, p.take_periods, p.dosage_per_time, p.dosage_unit, p.take_timing, " +
            "m.medicine_name, m.approval_number, m.brand_name, m.specification, m.unit_per_box, " +
            "u.real_name " +
            "FROM stock s " +
            "LEFT JOIN prescription p ON s.prescription_id = p.prescription_id AND p.deleted = 0 " +
            "LEFT JOIN medicine m ON p.medicine_id = m.medicine_id AND m.deleted = 0 " +
            "LEFT JOIN sys_user u ON p.user_id = u.user_id AND u.deleted = 0 " +
            "WHERE s.deleted = 0 AND p.status = 1 AND m.status = 1")
    List<Map<String, Object>> selectStockWithDetail();

    @Select("SELECT s.*, p.user_id, p.medicine_id, p.daily_consumption, " +
            "p.take_frequency_code, p.take_periods, p.dosage_per_time, p.dosage_unit, p.take_timing, " +
            "m.medicine_name, m.approval_number, m.brand_name, m.specification, m.unit_per_box, " +
            "u.real_name " +
            "FROM stock s " +
            "LEFT JOIN prescription p ON s.prescription_id = p.prescription_id AND p.deleted = 0 " +
            "LEFT JOIN medicine m ON p.medicine_id = m.medicine_id AND m.deleted = 0 " +
            "LEFT JOIN sys_user u ON p.user_id = u.user_id AND u.deleted = 0 " +
            "WHERE s.deleted = 0 AND p.status = 1 AND m.status = 1 AND p.user_id = #{userId}")
    List<Map<String, Object>> selectStockWithDetailByUserId(Long userId);
}

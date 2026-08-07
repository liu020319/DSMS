package com.medicine.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.medicine.entity.Prescription;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface PrescriptionMapper extends BaseMapper<Prescription> {

    Page<Prescription> selectPageWithRealName(
            Page<Prescription> page,
            @Param("userId") Long userId,
            @Param("medicineId") Long medicineId,
            @Param("realName") String realName,
            @Param("allowedUserIds") java.util.List<Long> allowedUserIds
    );
}

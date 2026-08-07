package com.medicine.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("medicine")
public class Medicine {
    @TableId(type = IdType.AUTO)
    private Long medicineId;
    private String approvalNumber;
    private String medicineName;
    private String brandName;
    private String specification;
    private Integer unitPerBox;
    private String boxUnit;
    private String manufacturer;
    private BigDecimal referencePrice;
    private String imageUrl;
    private Integer status;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
    @TableLogic
    private Integer deleted;
}

package com.medicine.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("stock")
public class Stock {
    @TableId(type = IdType.AUTO)
    private Long stockId;
    private Long prescriptionId;
    private LocalDateTime lastCalcTime;
    private Integer totalRemainingUnits;
    private Integer remainingDays;
    private String todayDeductedPeriods;
    private LocalDate lastDeductionDate;
    @Version
    private Integer version;
    private LocalDate expiryDate;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
    @TableLogic
    private Integer deleted;
}

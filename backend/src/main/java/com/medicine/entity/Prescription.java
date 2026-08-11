package com.medicine.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("prescription")
public class Prescription {
    @TableId(type = IdType.AUTO)
    private Long prescriptionId;
    private Long userId;
    private Long medicineId;
    private Integer dailyTimes;
    private Integer dosagePerTime;
    private String dosageUnit;
    private Integer dailyConsumption;
    private Integer daysPerBox;
    private String takeNotes;
    private String takeTiming;
    private String takeFrequencyCode;
    private String takePeriods;
    private Integer status;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
    @TableLogic
    private Integer deleted;
}

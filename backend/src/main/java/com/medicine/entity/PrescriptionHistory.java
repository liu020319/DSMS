package com.medicine.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("prescription_history")
public class PrescriptionHistory {
    @TableId(type = IdType.AUTO)
    private Long historyId;
    private Long prescriptionId;
    private Long userId;
    private Long medicineId;
    private Integer dailyTimes;
    private Integer dosagePerTime;
    private Integer dailyConsumption;
    private Integer daysPerBox;
    private String takeNotes;
    private String changeReason;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
    @TableLogic
    private Integer deleted;
}

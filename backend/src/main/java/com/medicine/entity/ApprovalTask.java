package com.medicine.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("approval_task")
public class ApprovalTask {
    @TableId(type = IdType.AUTO)
    private Long taskId;
    private Long applicantId;
    private Long handlerId;
    private String taskType;
    private String contentJson;
    private String status;
    private String handlerComment;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
    @TableLogic
    private Integer deleted;
}

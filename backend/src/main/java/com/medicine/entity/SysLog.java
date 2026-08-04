package com.medicine.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("sys_log")
public class SysLog {
    @TableId(type = IdType.AUTO)
    private Long logId;
    private Long userId;
    private String operationType;
    private String operationContent;
    private String operationIp;
    private LocalDateTime operationTime;
    @TableLogic
    private Integer deleted;
}

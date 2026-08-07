package com.medicine.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("logistics_event")
public class LogisticsEvent {
    @TableId(type = IdType.AUTO)
    private Long eventId;
    private Long orderId;
    private String statusCode;
    private String statusText;
    private String detail;
    private LocalDateTime occurredTime;
    private String source;
    private LocalDateTime createTime;
    @TableLogic
    private Integer deleted;
}

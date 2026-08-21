package com.medicine.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("software_service_work_order")
public class SoftwareServiceWorkOrder {
    @TableId(type = IdType.AUTO)
    private Long workOrderId;
    private Long requestId;
    private Long requesterUserId;
    private String workOrderType;
    private String subject;
    private String description;
    private String status;
    private Long handlerUserId;
    private String resolutionText;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
    private LocalDateTime resolvedTime;
    @TableLogic
    private Integer deleted;
}

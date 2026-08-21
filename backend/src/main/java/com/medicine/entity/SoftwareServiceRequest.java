package com.medicine.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("software_service_request")
public class SoftwareServiceRequest {
    @TableId(type = IdType.AUTO)
    private Long requestId;
    private Long requesterUserId;
    private String serviceType;
    private String title;
    private String requirementText;
    private String technologyStack;
    private String budgetRange;
    private LocalDate expectedDate;
    private String contactChannel;
    private String status;
    private BigDecimal quoteAmount;
    private String managerNote;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
    @TableLogic
    private Integer deleted;
}

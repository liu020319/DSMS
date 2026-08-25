package com.medicine.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("purchase_evidence")
public class PurchaseEvidence {
    @TableId(type = IdType.AUTO)
    private Long evidenceId;
    private Long orderId;
    private Long purchaseId;
    private Long elderId;
    private Long parentId;
    private String evidenceType;
    private Long fileId;
    private String title;
    private LocalDateTime occurredTime;
    private BigDecimal amount;
    private String purchasePlatform;
    private String note;
    private Long createdBy;
    private LocalDateTime createTime;
    @TableLogic
    private Integer deleted;
}

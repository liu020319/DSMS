package com.medicine.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("family_fund_transaction")
public class FamilyFundTransaction {
    @TableId(type = IdType.AUTO)
    private Long transactionId;
    private Long elderId;
    private Long parentId;
    private String transactionType;
    private BigDecimal amount;
    private String paymentPlatform;
    private LocalDateTime transactionTime;
    private Long referenceOrderId;
    private String proofUrl;
    private String note;
    private LocalDateTime createTime;
    @TableLogic
    private Integer deleted;
}

package com.medicine.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("order_receipt_verification")
public class OrderReceiptVerification {
    @TableId(type = IdType.AUTO)
    private Long verificationId;
    private Long orderId;
    private Long elderId;
    private String checkResult;
    private String photoUrl;
    private String checkJson;
    private String note;
    private LocalDateTime createTime;
    @TableLogic
    private Integer deleted;
}

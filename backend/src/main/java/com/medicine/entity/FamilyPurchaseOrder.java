package com.medicine.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("family_purchase_order")
public class FamilyPurchaseOrder {
    @TableId(type = IdType.AUTO)
    private Long orderId;
    private Long taskId;
    private Long elderId;
    private Long parentId;
    private String itemJson;
    private String purchasePlatform;
    private String purchaseChannel;
    private LocalDateTime orderTime;
    private BigDecimal actualTotal;
    private String screenshotUrl;
    private String carrierCode;
    private String carrierName;
    private String trackingNo;
    private String logisticsStatus;
    private String receiptStatus;
    private LocalDateTime receivedTime;
    private String note;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
    @TableLogic
    private Integer deleted;
}

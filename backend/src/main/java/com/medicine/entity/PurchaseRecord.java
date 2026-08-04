package com.medicine.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("purchase_record")
public class PurchaseRecord {
    @TableId(type = IdType.AUTO)
    private Long purchaseId;
    private Long userId;
    private Long prescriptionId;
    private LocalDate purchaseDate;
    private Integer quantityBoxes;
    private BigDecimal unitPrice;
    private BigDecimal totalPrice;
    private LocalDate expiryDate;
    private Long operatorId;
    private String purchasePlatform;
    private Integer receiptStatus;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
    @TableLogic
    private Integer deleted;
}

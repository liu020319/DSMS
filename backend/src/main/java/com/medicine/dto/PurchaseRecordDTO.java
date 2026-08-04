package com.medicine.dto;

import javax.validation.constraints.NotNull;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class PurchaseRecordDTO {
    private Long purchaseId;
    @NotNull(message = "用户ID不能为空")
    private Long userId;
    @NotNull(message = "方案ID不能为空")
    private Long prescriptionId;
    @NotNull(message = "购药日期不能为空")
    private LocalDate purchaseDate;
    @NotNull(message = "购买盒数不能为空")
    private Integer quantityBoxes;
    @NotNull(message = "单价不能为空")
    private BigDecimal unitPrice;
    @NotNull(message = "有效期不能为空")
    private LocalDate expiryDate;
    private Long operatorId;
    private String purchasePlatform;
    private Integer receiptStatus;
}

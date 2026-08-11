package com.medicine.dto;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.DecimalMin;
import javax.validation.constraints.Positive;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
public class PurchaseRecordDTO {
    private Long purchaseId;
    @NotNull(message = "用户ID不能为空")
    private Long userId;
    @NotNull(message = "方案ID不能为空")
    private Long prescriptionId;
    @NotNull(message = "购药日期不能为空")
    private LocalDate purchaseDate;
    private LocalDateTime purchaseTime;
    @NotNull(message = "购买盒数不能为空")
    @Positive(message = "购买盒数必须大于0")
    private Integer quantityBoxes;
    @NotNull(message = "单价不能为空")
    @DecimalMin(value = "0.0", inclusive = true, message = "单价不能小于0")
    private BigDecimal unitPrice;
    @NotNull(message = "有效期不能为空")
    private LocalDate expiryDate;
    private Long operatorId;
    private String purchasePlatform;
    private String purchaseChannel;
    private Long orderId;
    private String proofUrl;
    private Integer receiptStatus;
}

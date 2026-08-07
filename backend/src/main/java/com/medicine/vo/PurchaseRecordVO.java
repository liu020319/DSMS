package com.medicine.vo;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
public class PurchaseRecordVO {
    private Long purchaseId;
    private Long userId;
    private Long prescriptionId;
    private LocalDate purchaseDate;
    private LocalDateTime purchaseTime;
    private Integer quantityBoxes;
    private BigDecimal unitPrice;
    private BigDecimal totalPrice;
    private LocalDate expiryDate;
    private Long operatorId;
    private String purchasePlatform;
    private String purchaseChannel;
    private Long orderId;
    private String proofUrl;
    private Integer receiptStatus;
    private String medicineName;
    private String approvalNumber;
    private String brandName;
    private String specification;
    private String userName;
    private String operatorName;
}

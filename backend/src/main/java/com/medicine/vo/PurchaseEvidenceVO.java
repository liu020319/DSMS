package com.medicine.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class PurchaseEvidenceVO {
    private Long evidenceId;
    private Long orderId;
    private Long elderId;
    private String evidenceType;
    private String evidenceTypeText;
    private Long fileId;
    private String fileUrl;
    private String title;
    private LocalDateTime occurredTime;
    private BigDecimal amount;
    private String purchasePlatform;
    private String note;
    private Long createdBy;
    private LocalDateTime createTime;
}

package com.medicine.dto;

import lombok.Data;

import javax.validation.constraints.DecimalMin;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class PurchaseEvidenceDTO {
    @NotBlank(message = "请选择凭证类型")
    private String evidenceType;

    @NotNull(message = "请先上传凭证图片")
    private Long fileId;

    @Size(max = 120, message = "凭证标题不能超过120字")
    private String title;

    @NotNull(message = "请选择凭证发生时间")
    private LocalDateTime occurredTime;

    @DecimalMin(value = "0.00", message = "凭证金额不能小于0")
    private BigDecimal amount;

    @Size(max = 100, message = "购药平台不能超过100字")
    private String purchasePlatform;

    @Size(max = 500, message = "凭证说明不能超过500字")
    private String note;
}

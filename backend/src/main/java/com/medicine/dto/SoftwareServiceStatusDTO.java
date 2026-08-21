package com.medicine.dto;

import lombok.Data;

import javax.validation.constraints.DecimalMin;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;
import java.math.BigDecimal;

@Data
public class SoftwareServiceStatusDTO {
    @NotBlank(message = "状态不能为空")
    private String status;
    @DecimalMin(value = "0.00", message = "报价不能为负数")
    private BigDecimal quoteAmount;
    @Size(max = 1000, message = "处理说明最多1000个字符")
    private String managerNote;
}

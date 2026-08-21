package com.medicine.dto;

import lombok.Data;

import javax.validation.constraints.DecimalMin;
import javax.validation.constraints.DecimalMax;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.math.BigDecimal;

@Data
public class PersonalAccountDTO {
    @NotNull(message = "账本不能为空")
    private Long ledgerId;
    @NotBlank(message = "账户名称不能为空")
    @Size(max = 80, message = "账户名称最多80个字符")
    private String accountName;
    @NotBlank(message = "账户类型不能为空")
    private String accountType;
    @NotNull(message = "初始余额不能为空")
    @DecimalMin(value = "-999999999999.99", message = "初始余额超出范围")
    @DecimalMax(value = "999999999999.99", message = "初始余额超出范围")
    private BigDecimal initialBalance;
}

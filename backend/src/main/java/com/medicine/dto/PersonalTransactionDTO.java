package com.medicine.dto;

import lombok.Data;

import javax.validation.constraints.DecimalMin;
import javax.validation.constraints.DecimalMax;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class PersonalTransactionDTO {
    @NotNull(message = "账本不能为空")
    private Long ledgerId;
    @NotNull(message = "账户不能为空")
    private Long accountId;
    @NotBlank(message = "收支类型不能为空")
    private String transactionType;
    @NotBlank(message = "分类不能为空")
    @Size(max = 50, message = "分类最多50个字符")
    private String categoryName;
    @NotNull(message = "金额不能为空")
    @DecimalMin(value = "0.01", message = "金额必须大于0")
    @DecimalMax(value = "999999999999.99", message = "金额超出范围")
    private BigDecimal amount;
    @NotNull(message = "交易时间不能为空")
    private LocalDateTime transactionTime;
    @Size(max = 100, message = "交易对象最多100个字符")
    private String counterparty;
    @Size(max = 500, message = "备注最多500个字符")
    private String note;
}

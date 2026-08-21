package com.medicine.dto;

import lombok.Data;

import javax.validation.constraints.DecimalMin;
import javax.validation.constraints.DecimalMax;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;
import java.math.BigDecimal;

@Data
public class PersonalBudgetDTO {
    @NotNull(message = "账本不能为空")
    private Long ledgerId;
    @NotBlank(message = "预算月份不能为空")
    @Pattern(regexp = "^\\d{4}-(0[1-9]|1[0-2])$", message = "预算月份格式应为yyyy-MM")
    private String budgetMonth;
    @NotBlank(message = "分类不能为空")
    @Size(max = 50, message = "分类最多50个字符")
    private String categoryName;
    @NotNull(message = "预算金额不能为空")
    @DecimalMin(value = "0.01", message = "预算金额必须大于0")
    @DecimalMax(value = "999999999999.99", message = "预算金额超出范围")
    private BigDecimal budgetAmount;
}

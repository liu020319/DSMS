package com.medicine.vo;
import com.alibaba.excel.annotation.ExcelProperty;
import lombok.Data;
import java.math.BigDecimal;
@Data
public class ExpenseSummaryExportVO {
    @ExcelProperty("统计维度") private String dimension;
    @ExcelProperty("购药金额(元)") private BigDecimal totalAmount;
    @ExcelProperty("购药次数") private Long purchaseCount;
    @ExcelProperty("次均花费(元)") private BigDecimal averageAmount;
}

package com.medicine.vo;

import com.alibaba.excel.annotation.ExcelProperty;
import lombok.Data;
import java.math.BigDecimal;

@Data
public class MedicineExportVO {
    @ExcelProperty("药品ID")
    private Long medicineId;
    @ExcelProperty("国药准字号")
    private String approvalNumber;
    @ExcelProperty("药品通用名")
    private String medicineName;
    @ExcelProperty("品牌名")
    private String brandName;
    @ExcelProperty("规格")
    private String specification;
    @ExcelProperty("每盒单位数")
    private Integer unitPerBox;
    @ExcelProperty("生产厂家")
    private String manufacturer;
    @ExcelProperty("参考价格(元)")
    private BigDecimal referencePrice;
    @ExcelProperty("状态")
    private String statusText;
}

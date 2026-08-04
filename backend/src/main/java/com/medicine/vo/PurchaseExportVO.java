package com.medicine.vo;

import com.alibaba.excel.annotation.ExcelProperty;
import lombok.Data;
import java.math.BigDecimal;

@Data
public class PurchaseExportVO {
    @ExcelProperty("购药记录ID")
    private Long purchaseId;
    @ExcelProperty("用户姓名")
    private String userName;
    @ExcelProperty("药品名称")
    private String medicineName;
    @ExcelProperty("国药准字号")
    private String approvalNumber;
    @ExcelProperty("品牌名")
    private String brandName;
    @ExcelProperty("规格")
    private String specification;
    @ExcelProperty("购药日期")
    private String purchaseDate;
    @ExcelProperty("购买盒数")
    private Integer quantityBoxes;
    @ExcelProperty("单价(元)")
    private BigDecimal unitPrice;
    @ExcelProperty("总价(元)")
    private BigDecimal totalPrice;
    @ExcelProperty("有效期")
    private String expiryDate;
    @ExcelProperty("购药平台")
    private String purchasePlatform;
    @ExcelProperty("操作人")
    private String operatorName;
}

package com.medicine.vo;

import com.alibaba.excel.annotation.ExcelProperty;
import lombok.Data;

@Data
public class LogExportVO {
    @ExcelProperty("日志ID")
    private Long logId;
    @ExcelProperty("用户ID")
    private Long userId;
    @ExcelProperty("操作类型")
    private String operationType;
    @ExcelProperty("操作内容")
    private String operationContent;
    @ExcelProperty("操作IP")
    private String operationIp;
    @ExcelProperty("操作时间")
    private String operationTime;
}

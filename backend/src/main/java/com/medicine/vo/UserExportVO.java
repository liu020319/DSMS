package com.medicine.vo;

import com.alibaba.excel.annotation.ExcelProperty;
import lombok.Data;

@Data
public class UserExportVO {
    @ExcelProperty("用户ID")
    private Long userId;
    @ExcelProperty("用户名")
    private String username;
    @ExcelProperty("真实姓名")
    private String realName;
    @ExcelProperty("手机号")
    private String phone;
    @ExcelProperty("角色")
    private String roleText;
    @ExcelProperty("状态")
    private String statusText;
}

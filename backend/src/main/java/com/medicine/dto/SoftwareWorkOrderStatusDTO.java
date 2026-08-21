package com.medicine.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

@Data
public class SoftwareWorkOrderStatusDTO {
    @NotBlank(message = "状态不能为空")
    private String status;
    @Size(max = 1000, message = "处理结果最多1000个字符")
    private String resolutionText;
}

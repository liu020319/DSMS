package com.medicine.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

@Data
public class SoftwareWorkOrderDTO {
    @NotNull(message = "服务需求不能为空")
    private Long requestId;
    @NotBlank(message = "工单类型不能为空")
    private String workOrderType;
    @NotBlank(message = "工单主题不能为空")
    @Size(max = 120, message = "工单主题最多120个字符")
    private String subject;
    @NotBlank(message = "问题说明不能为空")
    @Size(max = 5000, message = "问题说明最多5000个字符")
    private String description;
}

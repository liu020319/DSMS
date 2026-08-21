package com.medicine.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;
import java.time.LocalDate;

@Data
public class SoftwareServiceRequestDTO {
    @NotBlank(message = "服务类型不能为空")
    private String serviceType;
    @NotBlank(message = "需求标题不能为空")
    @Size(max = 120, message = "需求标题最多120个字符")
    private String title;
    @NotBlank(message = "需求说明不能为空")
    @Size(max = 5000, message = "需求说明最多5000个字符")
    private String requirementText;
    @Size(max = 300, message = "技术栈最多300个字符")
    private String technologyStack;
    @Size(max = 80, message = "预算范围最多80个字符")
    private String budgetRange;
    private LocalDate expectedDate;
    @Size(max = 120, message = "联系方式最多120个字符")
    private String contactChannel;
}

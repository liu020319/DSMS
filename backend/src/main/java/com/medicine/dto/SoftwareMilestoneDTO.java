package com.medicine.dto;

import lombok.Data;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;
import java.time.LocalDate;

@Data
public class SoftwareMilestoneDTO {
    @NotBlank(message = "里程碑名称不能为空")
    @Size(max = 120, message = "里程碑名称最多120个字符")
    private String milestoneName;
    @Size(max = 800, message = "里程碑说明最多800个字符")
    private String milestoneDescription;
    private LocalDate plannedDate;
    @NotBlank(message = "里程碑状态不能为空")
    private String status;
    @Min(value = 0, message = "排序值不能为负数")
    private Integer sortNo;
}

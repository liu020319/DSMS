package com.medicine.dto;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ApprovalTaskDTO {
    private Long taskId;
    @NotNull(message = "申请人ID不能为空")
    private Long applicantId;
    @NotNull(message = "审批人ID不能为空")
    private Long handlerId;
    @NotBlank(message = "任务类型不能为空")
    private String taskType;
    @NotBlank(message = "申请内容不能为空")
    private String contentJson;
    private String status;
    private String handlerComment;
}

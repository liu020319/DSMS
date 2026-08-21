package com.medicine.dto;

import lombok.Data;
import javax.validation.constraints.NotBlank;

@Data
public class PublicInquiryAdminStatusDTO {
    @NotBlank(message = "状态不能为空")
    private String status;
}

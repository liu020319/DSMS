package com.medicine.dto;

import lombok.Data;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

@Data
public class PublicInquiryAccessDTO {
    @NotBlank(message = "咨询编号不能为空")
    @Size(max = 40, message = "咨询编号过长")
    private String inquiryNo;
    @NotBlank(message = "访问码不能为空")
    @Size(max = 80, message = "访问码过长")
    private String accessCode;
}

package com.medicine.dto;

import lombok.Data;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

@Data
public class PublicInquiryAdminReplyDTO {
    @NotBlank(message = "回复内容不能为空")
    @Size(min = 2, max = 3000, message = "回复应为2到3000个字符")
    private String messageText;
}

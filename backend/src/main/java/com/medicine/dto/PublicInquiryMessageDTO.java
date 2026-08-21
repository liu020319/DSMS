package com.medicine.dto;

import lombok.Data;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

@Data
public class PublicInquiryMessageDTO extends PublicInquiryAccessDTO {
    @NotBlank(message = "留言内容不能为空")
    @Size(min = 2, max = 3000, message = "留言应为2到3000个字符")
    private String messageText;
    @NotBlank(message = "请先完成人机验证")
    private String humanToken;
}

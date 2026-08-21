package com.medicine.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;

@Data
public class PublicServiceInquiryDTO {
    @NotBlank(message = "称呼不能为空")
    @Size(max = 80, message = "称呼最多80个字符")
    private String contactName;

    @NotBlank(message = "联系方式不能为空")
    @Size(max = 160, message = "联系方式最多160个字符")
    private String contactValue;

    @NotBlank(message = "服务类型不能为空")
    private String serviceType;

    @Size(max = 80, message = "项目类型最多80个字符")
    private String projectType;

    @NotBlank(message = "需求说明不能为空")
    @Size(min = 10, max = 3000, message = "需求说明应为10到3000个字符")
    private String inquiryText;

    @Size(max = 120, message = "来源路径过长")
    @Pattern(regexp = "^/[A-Za-z0-9_/#?&=.-]*$", message = "来源路径格式不正确")
    private String sourcePath;

    @NotBlank(message = "请先完成人机验证")
    private String humanToken;
}

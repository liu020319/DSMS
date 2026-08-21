package com.medicine.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;

@Data
public class PortalRegisterDTO {
    @NotBlank(message = "用户名不能为空")
    @Size(min = 4, max = 50, message = "用户名长度应为4到50位")
    @Pattern(regexp = "[A-Za-z0-9_]+", message = "用户名只能包含字母、数字和下划线")
    private String username;

    @NotBlank(message = "密码不能为空")
    @Size(min = 8, max = 64, message = "密码长度应为8到64位")
    private String password;

    @NotBlank(message = "显示名称不能为空")
    @Size(max = 50, message = "显示名称不能超过50位")
    private String displayName;

    @NotBlank(message = "邀请码不能为空")
    @Size(max = 64, message = "邀请码不能超过64位")
    private String inviteCode;

    @NotBlank(message = "请先完成人机验证")
    private String humanToken;
}

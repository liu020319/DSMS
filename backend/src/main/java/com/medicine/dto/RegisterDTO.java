package com.medicine.dto;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import javax.validation.constraints.Pattern;
import lombok.Data;

@Data
public class RegisterDTO {
    @NotBlank(message = "用户名不能为空")
    @Size(max = 50, message = "用户名不能超过50位")
    private String username;
    @NotBlank(message = "密码不能为空")
    @Size(min = 8, max = 64, message = "密码长度应为8到64位")
    private String password;
    @NotBlank(message = "真实姓名不能为空")
    private String realName;
    private String phone;
    @javax.validation.constraints.Email(message = "邮箱格式不正确")
    private String email;
    @NotBlank(message = "角色不能为空")
    @Pattern(regexp = "ADMIN|ELDER", message = "角色类型不正确")
    private String role;
    private Long bindParentId;
}

package com.medicine.vo;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class LoginVO {
    private String token;
    private Long userId;
    private String username;
    private String realName;
    private String role;
    private Long bindParentId;
}

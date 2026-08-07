package com.medicine.dto;

import lombok.Data;
import javax.validation.constraints.NotBlank;

@Data
public class HumanVerifyDTO {
    @NotBlank(message = "验证请求已失效，请刷新后重试")
    private String challengeId;
}

package com.medicine.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@AllArgsConstructor
public class HumanVerifyVO {
    private String humanToken;
    private LocalDateTime expiresAt;
}

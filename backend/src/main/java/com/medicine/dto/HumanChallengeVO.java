package com.medicine.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@AllArgsConstructor
public class HumanChallengeVO {
    private String challengeId;
    private LocalDateTime expiresAt;
}

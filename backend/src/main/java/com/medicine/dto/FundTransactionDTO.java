package com.medicine.dto;
import lombok.Data;
import javax.validation.constraints.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
@Data
public class FundTransactionDTO {
    @NotNull private Long elderId;
    @NotBlank private String transactionType;
    @NotNull private BigDecimal amount;
    private String paymentPlatform;
    @NotNull private LocalDateTime transactionTime;
    private String proofUrl;
    @Size(max = 500) private String note;
}

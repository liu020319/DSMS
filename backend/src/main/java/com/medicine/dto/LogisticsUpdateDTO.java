package com.medicine.dto;
import lombok.Data;
import javax.validation.constraints.*;
import java.time.LocalDateTime;
@Data
public class LogisticsUpdateDTO {
    @NotBlank @Pattern(regexp = "ORDERED|PICKED_UP|IN_TRANSIT|OUT_FOR_DELIVERY|DELIVERED|EXCEPTION") private String statusCode;
    @NotBlank @Size(max = 100) private String statusText;
    @Size(max = 500) private String detail;
    @NotNull private LocalDateTime occurredTime;
    @Size(max = 30) private String carrierCode;
    @Size(max = 60) private String carrierName;
    @Size(max = 80) private String trackingNo;
}

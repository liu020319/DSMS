package com.medicine.dto;
import lombok.Data;
import javax.validation.Valid;
import javax.validation.constraints.*;
import java.time.LocalDateTime;
import java.util.List;
@Data
public class FamilyOrderDTO {
    @NotNull private Long taskId;
    @NotBlank @Size(max = 100) private String purchasePlatform;
    @NotBlank @Pattern(regexp = "ONLINE|OFFLINE") private String purchaseChannel;
    @NotNull private LocalDateTime orderTime;
    private LocalDateTime expectedArrivalTime;
    @Size(max = 500) private String screenshotUrl;
    @Size(max = 30) private String carrierCode;
    @Size(max = 60) private String carrierName;
    @Size(max = 80) private String trackingNo;
    @Size(max = 500) private String note;
    @Valid @NotEmpty private List<OrderItemDTO> items;
}

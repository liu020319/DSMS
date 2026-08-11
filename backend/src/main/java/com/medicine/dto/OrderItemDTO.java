package com.medicine.dto;
import lombok.Data;
import javax.validation.constraints.*;
import java.math.BigDecimal;
import java.time.LocalDate;
@Data
public class OrderItemDTO {
    @NotNull private Long prescriptionId;
    @NotNull @Positive @Max(999) private Integer quantityBoxes;
    @NotNull @DecimalMin("0.00") @DecimalMax("100000.00") private BigDecimal unitPrice;
    @NotNull private LocalDate expiryDate;
}

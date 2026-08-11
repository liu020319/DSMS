package com.medicine.dto;
import lombok.Data;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Positive;
import javax.validation.constraints.Max;
@Data
public class PurchaseRequestItemDTO {
    @NotNull(message = "请选择用药方案")
    private Long prescriptionId;
    @NotNull(message = "请填写需要的盒数")
    @Positive(message = "盒数必须大于0")
    @Max(value = 999, message = "单种药品申请盒数不能超过999")
    private Integer quantityBoxes;
}

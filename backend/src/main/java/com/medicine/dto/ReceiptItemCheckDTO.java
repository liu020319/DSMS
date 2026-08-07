package com.medicine.dto;

import lombok.Data;
import javax.validation.constraints.*;

@Data
public class ReceiptItemCheckDTO {
    @NotNull(message = "药品方案不能为空")
    private Long prescriptionId;

    @NotNull(message = "请填写实收数量")
    @Min(value = 0, message = "实收数量不能小于0")
    private Integer receivedQuantityBoxes;

    @NotBlank(message = "请填写包装上的国药准字号")
    @Size(max = 80, message = "国药准字号过长")
    private String approvalNumber;

    @NotNull(message = "请确认外包装是否完好")
    private Boolean packageIntact;
}

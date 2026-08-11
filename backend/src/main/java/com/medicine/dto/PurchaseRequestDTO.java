package com.medicine.dto;
import lombok.Data;
import javax.validation.Valid;
import javax.validation.constraints.*;
import java.util.List;
@Data
public class PurchaseRequestDTO {
    @NotNull private Long elderId;
    @NotNull private Long parentId;
    @NotBlank(message = "请选择申请原因")
    @Pattern(regexp = "LOW_STOCK|LOST|NEW_PRESCRIPTION|TRAVEL|OTHER", message = "申请原因不正确")
    private String reason;
    @Size(max = 500, message = "说明不能超过500字") private String note;
    @Valid @NotEmpty(message = "至少选择一种药品") @Size(max = 20, message = "一次最多申请20种药品") private List<PurchaseRequestItemDTO> items;
    @AssertTrue(message = "请完成二次确认") private boolean confirmed;
}

package com.medicine.dto;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import lombok.Data;
import java.math.BigDecimal;

@Data
public class MedicineDTO {
    private Long medicineId;
    @NotBlank(message = "国药准字号不能为空")
    private String approvalNumber;
    @NotBlank(message = "药品通用名不能为空")
    private String medicineName;
    @NotBlank(message = "品牌名不能为空")
    private String brandName;
    @NotBlank(message = "规格不能为空")
    private String specification;
    @NotNull(message = "每盒单位数不能为空")
    private Integer unitPerBox;
    private String boxUnit;
    @NotBlank(message = "生产厂家不能为空")
    private String manufacturer;
    @NotNull(message = "参考价格不能为空")
    private BigDecimal referencePrice;
    private String imageUrl;
    private Integer status;
}

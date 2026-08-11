package com.medicine.dto;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Positive;
import lombok.Data;

@Data
public class PrescriptionDTO {
    private Long prescriptionId;
    @NotNull(message = "用户ID不能为空")
    private Long userId;
    @NotNull(message = "药品ID不能为空")
    private Long medicineId;
    @NotNull(message = "每日服药次数不能为空")
    @Positive(message = "每日服药次数必须大于0")
    private Integer dailyTimes;
    @NotNull(message = "每次用量不能为空")
    @Positive(message = "每次用量必须大于0")
    private Integer dosagePerTime;
    private String dosageUnit;
    private String takeNotes;
    private String takeTiming;
    private String takeFrequencyCode;
    private String takePeriods;
    private Integer status;
}

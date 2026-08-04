package com.medicine.vo;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
public class PrescriptionVO {
    private Long prescriptionId;
    private Long userId;
    private Long medicineId;
    private Integer dailyTimes;
    private Integer dosagePerTime;
    private String dosageUnit;
    private Integer dailyConsumption;
    private Integer daysPerBox;
    private String takeNotes;
    private String takeTiming;
    private String takeFrequencyCode;
    private String takeFrequencyLabel;
    private String takePeriods;
    private Integer status;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
    private String medicineName;
    private String approvalNumber;
    private String brandName;
    private String specification;
    private Integer unitPerBox;
    private BigDecimal referencePrice;
    private String manufacturer;
    private Integer totalRemainingUnits;
    private Integer remainingDays;
    private LocalDate expiryDate;
    private String realName;
    private String role;
    private String imageUrl;
}

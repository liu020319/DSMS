package com.medicine.vo;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
public class StockVO {
    private Long stockId;
    private Long prescriptionId;
    private LocalDateTime lastCalcTime;
    private Integer totalRemainingUnits;
    private Integer remainingDays;
    private String todayDeductedPeriods;
    private LocalDate lastDeductionDate;
    private Integer version;
    private LocalDate expiryDate;
    private Long userId;
    private Long medicineId;
    private Integer dailyConsumption;
    private String medicineName;
    private String approvalNumber;
    private String brandName;
    private String specification;
    private Integer unitPerBox;
    private Boolean isWarning;
    private Boolean isExpiring;
    private String realName;
    private String takeFrequencyCode;
    private String takeFrequencyLabel;
    private String takePeriods;
    private Integer dosagePerTime;
    private String dosageUnit;
    private String takeTiming;
}

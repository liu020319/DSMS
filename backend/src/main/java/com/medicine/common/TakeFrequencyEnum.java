package com.medicine.common;

import lombok.Getter;
import java.util.Arrays;
import java.util.List;

@Getter
public enum TakeFrequencyEnum {

    DAILY_1_MORNING("DAILY_1_MORNING", "一日1次晨服", 1, Arrays.asList("MORNING")),
    DAILY_1_NOON("DAILY_1_NOON", "一日1次午服", 1, Arrays.asList("NOON")),
    DAILY_1_EVENING("DAILY_1_EVENING", "一日1次晚服", 1, Arrays.asList("EVENING")),
    DAILY_2_MORNING_EVENING("DAILY_2_MORNING_EVENING", "一日2次早晚服", 2, Arrays.asList("MORNING", "EVENING")),
    DAILY_3_FULL_DAY("DAILY_3_FULL_DAY", "一日3次早中晚服", 3, Arrays.asList("MORNING", "NOON", "EVENING"));

    private final String code;
    private final String label;
    private final int dailyTimes;
    private final List<String> periods;

    TakeFrequencyEnum(String code, String label, int dailyTimes, List<String> periods) {
        this.code = code;
        this.label = label;
        this.dailyTimes = dailyTimes;
        this.periods = periods;
    }

    public static TakeFrequencyEnum fromCode(String code) {
        for (TakeFrequencyEnum e : values()) {
            if (e.getCode().equals(code)) {
                return e;
            }
        }
        return DAILY_1_MORNING;
    }

    public static String getLabelByCode(String code) {
        return fromCode(code).getLabel();
    }
}

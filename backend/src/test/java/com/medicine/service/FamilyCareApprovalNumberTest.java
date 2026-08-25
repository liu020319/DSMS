package com.medicine.service;

import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;

class FamilyCareApprovalNumberTest {
    private final FamilyCareService service = new FamilyCareService();

    @Test
    void acceptsNumberWithOrWithoutChinesePrefix() {
        assertEquals("420881", normalize("420881"));
        assertEquals("420881", normalize("国药准字420881"));
        assertEquals("H12345678", normalize(" 国药准字 h12345678 "));
    }

    private String normalize(String value) {
        return ReflectionTestUtils.invokeMethod(service, "normalizeApprovalNumber", value);
    }
}

package com.medicine.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class FlexibleLocalDateTimeDeserializerTest {
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        SimpleModule module = new SimpleModule();
        module.addDeserializer(LocalDateTime.class, new FlexibleLocalDateTimeDeserializer());
        objectMapper.registerModule(module);
    }

    @Test
    void acceptsIsoTimeUsedByBrowserForms() throws Exception {
        TimePayload payload = objectMapper.readValue("{\"value\":\"2026-08-24T12:34:56\"}", TimePayload.class);
        assertEquals(LocalDateTime.of(2026, 8, 24, 12, 34, 56), payload.value);
    }

    @Test
    void acceptsLegacySpaceSeparatedTime() throws Exception {
        TimePayload payload = objectMapper.readValue("{\"value\":\"2026-08-24 12:34:56\"}", TimePayload.class);
        assertEquals(LocalDateTime.of(2026, 8, 24, 12, 34, 56), payload.value);
    }

    @Test
    void rejectsUnknownTimeFormat() {
        assertThrows(Exception.class,
                () -> objectMapper.readValue("{\"value\":\"2026/08/24 12:34\"}", TimePayload.class));
    }

    public static class TimePayload {
        public LocalDateTime value;
    }
}

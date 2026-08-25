package com.medicine.config;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

/**
 * 同时兼容浏览器常用的 ISO 时间（T 分隔）和系统历史接口的空格分隔时间。
 */
public class FlexibleLocalDateTimeDeserializer extends StdDeserializer<LocalDateTime> {
    private static final DateTimeFormatter LEGACY_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public FlexibleLocalDateTimeDeserializer() {
        super(LocalDateTime.class);
    }

    @Override
    public LocalDateTime deserialize(JsonParser parser, DeserializationContext context) throws IOException {
        String value = parser.getValueAsString();
        if (value == null || value.trim().isEmpty()) return null;
        String normalized = value.trim();
        try {
            return LocalDateTime.parse(normalized, DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        } catch (DateTimeParseException ignored) {
            try {
                return LocalDateTime.parse(normalized, LEGACY_FORMATTER);
            } catch (DateTimeParseException ex) {
                throw JsonMappingException.from(parser,
                        "日期时间格式不正确，应为 yyyy-MM-ddTHH:mm:ss 或 yyyy-MM-dd HH:mm:ss", ex);
            }
        }
    }
}

package com.medicine.controller;

import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UploadControllerTest {
    private final UploadController controller = new UploadController();

    @Test
    void acceptsSupportedImageSignatures() {
        assertTrue(matches(new byte[]{(byte) 0xff, (byte) 0xd8, (byte) 0xff, 0, 0, 0, 0, 0, 0, 0, 0, 0}, ".jpg"));
        assertTrue(matches(new byte[]{(byte) 0x89, 0x50, 0x4e, 0x47, 0, 0, 0, 0, 0, 0, 0, 0}, ".png"));
        assertTrue(matches(new byte[]{'G', 'I', 'F', 0, 0, 0, 0, 0, 0, 0, 0, 0}, ".gif"));
        assertTrue(matches(new byte[]{'R', 'I', 'F', 'F', 0, 0, 0, 0, 'W', 'E', 'B', 'P'}, ".webp"));
    }

    @Test
    void rejectsDisguisedOrTruncatedFiles() {
        assertFalse(matches("not-an-image".getBytes(), ".png"));
        assertFalse(matches(new byte[]{(byte) 0x89, 0x50, 0x4e}, ".png"));
        assertFalse(matches(new byte[12], ".svg"));
    }

    private boolean matches(byte[] content, String extension) {
        return Boolean.TRUE.equals(ReflectionTestUtils.invokeMethod(
                controller, "matchesImageSignature", content, extension));
    }
}

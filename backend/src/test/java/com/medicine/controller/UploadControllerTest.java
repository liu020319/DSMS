package com.medicine.controller;

import com.medicine.service.FileAssetService;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

class UploadControllerTest {
    private final FileAssetService service = new FileAssetService(
            null, null, null, null, 5 * 1024 * 1024L, "dsms/test");

    @Test
    void acceptsSupportedImageSignatures() {
        assertNotNull(detect(new byte[]{(byte) 0xff, (byte) 0xd8, (byte) 0xff, 0, 0, 0, 0, 0, 0, 0, 0, 0}));
        assertNotNull(detect(new byte[]{(byte) 0x89, 0x50, 0x4e, 0x47, 0, 0, 0, 0, 0, 0, 0, 0}));
        assertNotNull(detect(new byte[]{'G', 'I', 'F', 0, 0, 0, 0, 0, 0, 0, 0, 0}));
        assertNotNull(detect(new byte[]{'R', 'I', 'F', 'F', 0, 0, 0, 0, 'W', 'E', 'B', 'P'}));
    }

    @Test
    void rejectsDisguisedOrTruncatedFiles() {
        assertNull(detect("not-an-image".getBytes()));
        assertNull(detect(new byte[]{(byte) 0x89, 0x50, 0x4e}));
        assertNull(detect(new byte[12]));
    }

    private Object detect(byte[] content) {
        return ReflectionTestUtils.invokeMethod(service, "detectImage", content);
    }
}

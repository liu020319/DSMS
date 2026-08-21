package com.medicine.storage;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.charset.StandardCharsets;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class LocalFileStorageTest {
    @TempDir Path tempDir;

    @Test
    void storesAndReadsObjectInsideConfiguredDirectory() throws Exception {
        LocalFileStorage storage = new LocalFileStorage(tempDir.toString());
        byte[] expected = "test-image".getBytes(StandardCharsets.UTF_8);
        storage.put("dsms/test/file.bin", expected);
        try (StoredContent content = storage.get("dsms/test/file.bin")) {
            byte[] actual = new byte[expected.length];
            int read = content.getInputStream().read(actual);
            assertArrayEquals(expected, read == expected.length ? actual : new byte[0]);
        }
    }

    @Test
    void rejectsPathTraversal() {
        LocalFileStorage storage = new LocalFileStorage(tempDir.toString());
        assertThrows(IllegalArgumentException.class,
                () -> storage.put("../../outside.bin", new byte[]{1}));
    }
}

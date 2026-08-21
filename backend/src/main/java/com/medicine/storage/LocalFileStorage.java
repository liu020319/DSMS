package com.medicine.storage;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@Component
public class LocalFileStorage implements FileStorage {
    private final Path baseDirectory;

    public LocalFileStorage(@Value("${storage.local.base-dir:./uploads/assets}") String baseDirectory) {
        this.baseDirectory = Paths.get(baseDirectory).toAbsolutePath().normalize();
    }

    @Override
    public String provider() {
        return "LOCAL";
    }

    @Override
    public void put(String objectKey, byte[] content) throws IOException {
        Path target = safePath(objectKey);
        Files.createDirectories(target.getParent());
        Files.write(target, content);
    }

    @Override
    public StoredContent get(String objectKey) throws IOException {
        return new StoredContent(new ByteArrayInputStream(Files.readAllBytes(safePath(objectKey))));
    }

    @Override
    public void delete(String objectKey) throws IOException {
        Files.deleteIfExists(safePath(objectKey));
    }

    private Path safePath(String objectKey) {
        Path target = baseDirectory.resolve(objectKey).normalize();
        if (!target.startsWith(baseDirectory)) {
            throw new IllegalArgumentException("非法文件路径");
        }
        return target;
    }
}

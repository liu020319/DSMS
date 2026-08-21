package com.medicine.storage;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Component
public class FileStorageRouter {
    private final Map<String, FileStorage> storages = new LinkedHashMap<>();
    private final String activeProvider;

    public FileStorageRouter(List<FileStorage> candidates,
                             @Value("${storage.provider:local}") String activeProvider) {
        for (FileStorage candidate : candidates) storages.put(candidate.provider(), candidate);
        this.activeProvider = normalize(activeProvider);
        if (!storages.containsKey(this.activeProvider)) {
            throw new IllegalStateException("不支持的文件存储类型: " + activeProvider);
        }
    }

    public FileStorage active() {
        return storages.get(activeProvider);
    }

    public FileStorage byProvider(String provider) {
        FileStorage storage = storages.get(normalize(provider));
        if (storage == null) throw new IllegalStateException("文件存储实现不存在: " + provider);
        return storage;
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim().toUpperCase(Locale.ROOT);
    }
}

package com.medicine.storage;

import com.obs.services.ObsClient;
import com.obs.services.model.ObsObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.annotation.PreDestroy;
import java.io.ByteArrayInputStream;
import java.io.IOException;

@Component
public class ObsFileStorage implements FileStorage {
    private final String endpoint;
    private final String bucket;
    private final String accessKey;
    private final String secretKey;
    private volatile ObsClient client;

    public ObsFileStorage(@Value("${storage.obs.endpoint:}") String endpoint,
                          @Value("${storage.obs.bucket:}") String bucket,
                          @Value("${storage.obs.access-key:}") String accessKey,
                          @Value("${storage.obs.secret-key:}") String secretKey) {
        this.endpoint = endpoint;
        this.bucket = bucket;
        this.accessKey = accessKey;
        this.secretKey = secretKey;
    }

    @Override
    public String provider() {
        return "OBS";
    }

    public String bucket() {
        return bucket;
    }

    @Override
    public void put(String objectKey, byte[] content) {
        client().putObject(bucket, objectKey, new ByteArrayInputStream(content));
    }

    @Override
    public StoredContent get(String objectKey) {
        ObsObject object = client().getObject(bucket, objectKey);
        return new StoredContent(object.getObjectContent());
    }

    @Override
    public void delete(String objectKey) {
        client().deleteObject(bucket, objectKey);
    }

    private ObsClient client() {
        if (isBlank(endpoint) || isBlank(bucket) || isBlank(accessKey) || isBlank(secretKey)) {
            throw new IllegalStateException("OBS 尚未配置，请检查服务器环境变量");
        }
        if (client == null) {
            synchronized (this) {
                if (client == null) client = new ObsClient(accessKey, secretKey, endpoint);
            }
        }
        return client;
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    @PreDestroy
    public void close() throws IOException {
        if (client != null) client.close();
    }
}

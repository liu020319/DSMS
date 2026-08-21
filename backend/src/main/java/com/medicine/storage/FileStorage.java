package com.medicine.storage;

import java.io.IOException;
import java.io.InputStream;

public interface FileStorage {
    String provider();

    void put(String objectKey, byte[] content) throws IOException;

    StoredContent get(String objectKey) throws IOException;

    void delete(String objectKey) throws IOException;
}

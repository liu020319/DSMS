package com.medicine.storage;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.io.IOException;
import java.io.InputStream;

@Getter
@AllArgsConstructor
public class StoredContent implements AutoCloseable {
    private final InputStream inputStream;

    @Override
    public void close() throws IOException {
        inputStream.close();
    }
}

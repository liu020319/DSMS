package com.medicine.vo;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class FileUploadVO {
    private Long fileId;
    private String url;
    private String category;
    private String contentType;
    private Long fileSize;
    private String sha256;
}

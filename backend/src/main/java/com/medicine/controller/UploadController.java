package com.medicine.controller;

import com.medicine.common.Result;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.util.UUID;

@RestController
@RequestMapping("/upload")
public class UploadController {

    @PostMapping("/image")
    public Result<String> uploadImage(@RequestParam("file") MultipartFile file,
                                       @RequestParam(required = false) String approvalNumber,
                                       @RequestParam(required = false) String medicineName) throws IOException {
        if (file.isEmpty()) {
            return Result.error("请选择文件");
        }
        String originalFilename = file.getOriginalFilename();
        String ext = originalFilename != null && originalFilename.contains(".")
                ? originalFilename.substring(originalFilename.lastIndexOf("."))
                : ".jpg";
        String prefix = "";
        if (approvalNumber != null && !approvalNumber.isEmpty()) {
            prefix = approvalNumber.replaceAll("[^a-zA-Z0-9\\u4e00-\\u9fa5]", "_") + "_";
        }
        if (medicineName != null && !medicineName.isEmpty()) {
            prefix += medicineName.replaceAll("[^a-zA-Z0-9\\u4e00-\\u9fa5]", "_") + "_";
        }
        String filename = prefix + UUID.randomUUID().toString().substring(0, 8) + ext;

        File uploadDir = new File("./uploads");
        if (!uploadDir.exists()) {
            uploadDir.mkdirs();
        }
        File dest = new File(uploadDir, filename);
        file.transferTo(dest.getAbsoluteFile());

        String url = "/api/uploads/" + filename;
        return Result.success(url);
    }
}

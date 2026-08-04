package com.medicine.controller;

import com.medicine.common.Result;
import com.medicine.common.BusinessException;
import com.medicine.util.AccessControl;
import javax.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.util.UUID;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

@RestController
@RequestMapping("/upload")
public class UploadController {

    private static final long MAX_FILE_SIZE = 5 * 1024 * 1024;
    private static final Set<String> ALLOWED_EXTENSIONS = new HashSet<>(Arrays.asList(".jpg", ".jpeg", ".png", ".gif", ".webp"));

    @org.springframework.beans.factory.annotation.Autowired
    private AccessControl accessControl;

    @PostMapping("/image")
    public Result<String> uploadImage(@RequestParam("file") MultipartFile file,
                                       @RequestParam(required = false) String approvalNumber,
                                        @RequestParam(required = false) String medicineName,
                                        HttpServletRequest request) throws IOException {
        accessControl.requireAdmin(request);
        if (file.isEmpty()) {
            return Result.error("请选择文件");
        }
        String originalFilename = file.getOriginalFilename();
        String ext = originalFilename != null && originalFilename.contains(".")
                ? originalFilename.substring(originalFilename.lastIndexOf("."))
                : ".jpg";
        ext = ext.toLowerCase();
        if (!ALLOWED_EXTENSIONS.contains(ext)) {
            throw new BusinessException(400, "仅支持 JPG、PNG、GIF、WEBP 图片");
        }
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new BusinessException(400, "图片大小不能超过5MB");
        }
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

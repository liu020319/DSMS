package com.medicine.controller;

import com.medicine.common.Result;
import com.medicine.common.BusinessException;
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

    @PostMapping("/image")
    public Result<String> uploadImage(@RequestParam("file") MultipartFile file,
                                       @RequestParam(required = false) String approvalNumber,
                                        @RequestParam(required = false) String medicineName,
                                        HttpServletRequest request) throws IOException {
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
        byte[] content = file.getBytes();
        if (!matchesImageSignature(content, ext)) {
            throw new BusinessException(400, "图片内容与文件格式不一致，请重新拍照或选择图片");
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
        if (!uploadDir.exists() && !uploadDir.mkdirs()) {
            throw new BusinessException("图片保存目录创建失败");
        }
        File dest = new File(uploadDir, filename);
        file.transferTo(dest.getAbsoluteFile());

        String url = "/api/uploads/" + filename;
        return Result.success(url);
    }

    private boolean matchesImageSignature(byte[] data, String extension) {
        if (data == null || data.length < 12) return false;
        if (".jpg".equals(extension) || ".jpeg".equals(extension)) {
            return (data[0] & 0xff) == 0xff && (data[1] & 0xff) == 0xd8 && (data[2] & 0xff) == 0xff;
        }
        if (".png".equals(extension)) {
            return (data[0] & 0xff) == 0x89 && data[1] == 0x50 && data[2] == 0x4e && data[3] == 0x47;
        }
        if (".gif".equals(extension)) {
            return data[0] == 'G' && data[1] == 'I' && data[2] == 'F';
        }
        if (".webp".equals(extension)) {
            return data[0] == 'R' && data[1] == 'I' && data[2] == 'F' && data[3] == 'F'
                    && data[8] == 'W' && data[9] == 'E' && data[10] == 'B' && data[11] == 'P';
        }
        return false;
    }
}

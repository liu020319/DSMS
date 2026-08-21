package com.medicine.controller;

import com.medicine.common.Result;
import com.medicine.service.FileAssetService;
import com.medicine.vo.FileUploadVO;
import javax.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

/** 兼容旧前端的入口；新页面应使用 /files/images 并明确传 category。 */
@RestController
@RequestMapping("/upload")
public class UploadController {
    private final FileAssetService fileAssetService;

    public UploadController(FileAssetService fileAssetService) {
        this.fileAssetService = fileAssetService;
    }

    @PostMapping("/image")
    public Result<String> uploadImage(@RequestParam("file") MultipartFile file,
                                       @RequestParam(required = false) String approvalNumber,
                                        @RequestParam(required = false) String medicineName,
                                        @RequestParam(required = false) String category,
                                        @RequestParam(required = false) String businessType,
                                        @RequestParam(required = false) Long businessId,
                                        HttpServletRequest request) throws Exception {
        String effectiveCategory = category;
        if (effectiveCategory == null || effectiveCategory.trim().isEmpty()) {
            effectiveCategory = notBlank(approvalNumber) || notBlank(medicineName)
                    ? "MEDICINE_IMAGE" : "GENERAL_IMAGE";
        }
        FileUploadVO uploaded = fileAssetService.uploadImage(file, effectiveCategory, businessType, businessId,
                (Long) request.getAttribute("userId"), String.valueOf(request.getAttribute("role")));
        return Result.success(uploaded.getUrl());
    }

    private boolean notBlank(String value) {
        return value != null && !value.trim().isEmpty();
    }
}

package com.medicine.controller;

import com.medicine.common.Result;
import com.medicine.service.FileAssetService;
import com.medicine.vo.FileUploadVO;
import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.InputStream;

@RestController
@RequestMapping("/files")
public class FileAssetController {
    private final FileAssetService fileAssetService;

    public FileAssetController(FileAssetService fileAssetService) {
        this.fileAssetService = fileAssetService;
    }

    @PostMapping("/images")
    public Result<FileUploadVO> uploadImage(@RequestParam("file") MultipartFile file,
                                            @RequestParam String category,
                                            @RequestParam(required = false) String businessType,
                                            @RequestParam(required = false) Long businessId,
                                            HttpServletRequest request) throws Exception {
        return Result.success(fileAssetService.uploadImage(file, category, businessType, businessId,
                userId(request), role(request)));
    }

    @GetMapping("/{id}/content")
    public StreamingResponseBody content(@PathVariable("id") Long fileId,
                                         HttpServletRequest request,
                                         HttpServletResponse response) throws Exception {
        final FileAssetService.DownloadableFile file = fileAssetService.open(fileId, userId(request), role(request));
        response.setContentType(file.getAsset().getContentType());
        response.setContentLengthLong(file.getAsset().getFileSize());
        response.setHeader(HttpHeaders.CACHE_CONTROL, "private, max-age=300");
        response.setHeader("X-Content-Type-Options", "nosniff");
        return output -> {
            try (FileAssetService.DownloadableFile ignored = file;
                 InputStream input = file.getContent().getInputStream()) {
                byte[] buffer = new byte[8192];
                int length;
                while ((length = input.read(buffer)) != -1) output.write(buffer, 0, length);
            }
        };
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable("id") Long fileId, HttpServletRequest request) throws Exception {
        fileAssetService.delete(fileId, userId(request), role(request));
        return Result.success();
    }

    private Long userId(HttpServletRequest request) { return (Long) request.getAttribute("userId"); }
    private String role(HttpServletRequest request) { return String.valueOf(request.getAttribute("role")); }
}

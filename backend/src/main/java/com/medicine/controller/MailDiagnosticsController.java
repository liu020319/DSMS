package com.medicine.controller;

import com.medicine.common.BusinessException;
import com.medicine.common.Result;
import com.medicine.service.NotificationService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import java.util.Map;

@RestController
@RequestMapping("/portal/system/mail")
public class MailDiagnosticsController {
    private final NotificationService notificationService;

    public MailDiagnosticsController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @GetMapping("/status")
    public Result<Map<String, Object>> status(HttpServletRequest request) {
        requireAdmin(request);
        return Result.success(notificationService.mailDiagnostics());
    }

    @PostMapping("/test")
    public Result<Map<String, Object>> test(HttpServletRequest request) {
        requireAdmin(request);
        return Result.success(notificationService.sendTestEmail((Long) request.getAttribute("userId")));
    }

    private void requireAdmin(HttpServletRequest request) {
        if (!"ADMIN".equals(String.valueOf(request.getAttribute("role")))) {
            throw new BusinessException(403, "仅平台管理员可以检查邮件提醒配置");
        }
    }
}

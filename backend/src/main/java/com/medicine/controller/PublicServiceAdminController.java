package com.medicine.controller;

import com.medicine.common.Result;
import com.medicine.dto.PublicInquiryAdminReplyDTO;
import com.medicine.dto.PublicInquiryAdminStatusDTO;
import com.medicine.service.PublicServiceInquiryService;
import com.medicine.service.SysLogService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/portal/services/public-inquiries")
public class PublicServiceAdminController {
    private final PublicServiceInquiryService inquiryService;
    private final SysLogService sysLogService;

    public PublicServiceAdminController(PublicServiceInquiryService inquiryService,
                                        SysLogService sysLogService) {
        this.inquiryService = inquiryService;
        this.sysLogService = sysLogService;
    }

    @GetMapping
    public Result<List<Map<String, Object>>> list(@RequestParam(required = false) String status,
                                                  HttpServletRequest request) {
        return Result.success(inquiryService.adminList(role(request), status));
    }

    @GetMapping("/{id}")
    public Result<Map<String, Object>> detail(@PathVariable("id") Long id, HttpServletRequest request) {
        return Result.success(inquiryService.adminDetail(id, role(request)));
    }

    @PutMapping("/{id}/status")
    public Result<Void> status(@PathVariable("id") Long id,
                               @Valid @RequestBody PublicInquiryAdminStatusDTO dto,
                               HttpServletRequest request) {
        inquiryService.updateStatus(id, dto.getStatus(), role(request));
        log(request, "更新游客咨询状态", "咨询ID：" + id + "，状态：" + dto.getStatus());
        return Result.success();
    }

    @PostMapping("/{id}/replies")
    public Result<Void> reply(@PathVariable("id") Long id,
                              @Valid @RequestBody PublicInquiryAdminReplyDTO dto,
                              HttpServletRequest request) {
        inquiryService.adminReply(id, userId(request), dto.getMessageText(), role(request));
        log(request, "回复游客软件咨询", "咨询ID：" + id);
        return Result.success();
    }

    private Long userId(HttpServletRequest request) { return (Long) request.getAttribute("userId"); }
    private String role(HttpServletRequest request) { return String.valueOf(request.getAttribute("role")); }
    private void log(HttpServletRequest request, String type, String content) {
        sysLogService.log(userId(request), type, content, request.getRemoteAddr());
    }
}

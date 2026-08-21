package com.medicine.controller;

import com.medicine.common.Result;
import com.medicine.dto.PublicServiceInquiryDTO;
import com.medicine.dto.PublicInquiryAccessDTO;
import com.medicine.dto.PublicInquiryMessageDTO;
import com.medicine.service.PublicServiceInquiryService;
import com.medicine.util.ClientAddressResolver;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import java.util.Map;

@RestController
@RequestMapping("/public/services")
public class PublicServiceController {
    private final PublicServiceInquiryService inquiryService;
    private final ClientAddressResolver clientAddressResolver;

    public PublicServiceController(PublicServiceInquiryService inquiryService,
                                   ClientAddressResolver clientAddressResolver) {
        this.inquiryService = inquiryService;
        this.clientAddressResolver = clientAddressResolver;
    }

    @PostMapping("/inquiries")
    public Result<Map<String, String>> inquiry(@Valid @RequestBody PublicServiceInquiryDTO dto,
                                               HttpServletRequest request) {
        return Result.success(inquiryService.submit(dto, clientAddressResolver.resolve(request)));
    }

    @PostMapping("/inquiries/query")
    public Result<Map<String, Object>> query(@Valid @RequestBody PublicInquiryAccessDTO dto,
                                             HttpServletRequest request) {
        return Result.success(inquiryService.publicDetail(
                dto.getInquiryNo(), dto.getAccessCode(), clientAddressResolver.resolve(request)));
    }

    @PostMapping("/inquiries/messages")
    public Result<Void> message(@Valid @RequestBody PublicInquiryMessageDTO dto,
                                HttpServletRequest request) {
        inquiryService.addVisitorMessage(dto, clientAddressResolver.resolve(request));
        return Result.success();
    }
}

package com.buenws.buenws_backend.API.Controller;

import com.buenws.buenws_backend.API.Records.Records;
import com.buenws.buenws_backend.API.Service.InquiryService;
import com.buenws.buenws_backend.API.Service.RateLimitService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.buenws.buenws_backend.Util.RequestUtil;

@RestController
@RequestMapping("/api/inquiry")
public class InquiryController {
    private final InquiryService inquiryService;
    private final RateLimitService rateLimitService;

    public InquiryController(InquiryService inquiryService, RateLimitService rateLimitService) {
        this.inquiryService = inquiryService;
        this.rateLimitService = rateLimitService;
    }

    //Contact Form Submission
    @PostMapping("/contact-submissions")
    public ResponseEntity<Records.ApiResponse<Void>> submitContactForm(
            @Valid @RequestBody Records.FormSubmissionRequest formSubmissionRequest,
            HttpServletRequest request
    ) {
        rateLimitService.checkBucket("contact-submissions:" + RequestUtil.getClientIp(request), 10);
        return ResponseEntity.ok(inquiryService.submitContactForm(formSubmissionRequest));
    }
}

package com.buenws.buenws_backend.API.Controller;

import com.buenws.buenws_backend.API.Records.UserRecords;
import com.buenws.buenws_backend.API.Service.InquiryService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/inquiry")
public class InquiryController {
    private final InquiryService inquiryService;

    public InquiryController(InquiryService inquiryService) {
        this.inquiryService = inquiryService;
    }

    //Contact Form Submission
    @PostMapping("/contact-submissions")
    public ResponseEntity<UserRecords.ApiResponse<Void>> submitContactForm(@RequestBody UserRecords.FormSubmissionRequestRecord formSubmissionRequestRecord) {
        return ResponseEntity.ok(inquiryService.submitContactForm(formSubmissionRequestRecord));
    }
}

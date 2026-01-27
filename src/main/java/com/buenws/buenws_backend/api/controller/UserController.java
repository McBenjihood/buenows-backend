package com.buenws.buenws_backend.api.controller;

import com.buenws.buenws_backend.api.records.UserRecords;
import com.buenws.buenws_backend.api.service.InquiryService;
import com.buenws.buenws_backend.api.service.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/user")
public class UserController {

    private InquiryService inquiryService;

    public UserController(InquiryService inquiryService) {
        this.inquiryService = inquiryService;
    }

    @PostMapping("/contact-submissions")
    public ResponseEntity<UserRecords.FormSubmissionResponseRecord> submitContactForm(@RequestBody UserRecords.FormSubmissionRequestRecord formSubmissionRequestRecord) {
        return ResponseEntity.ok(inquiryService.submitContactForm(formSubmissionRequestRecord));
    }

    @PostMapping("/register")
    public ResponseEntity<UserRecords.RegisterResponseRecord> registerUser(@RequestBody UserRecords.RegisterRequestRecord registerRequestRecord){
        return ResponseEntity.ok(new UserRecords.RegisterResponseRecord(true, "User registered successfully"));
    }
}

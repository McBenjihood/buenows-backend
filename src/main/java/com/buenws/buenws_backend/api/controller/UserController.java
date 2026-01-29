package com.buenws.buenws_backend.api.controller;

import com.buenws.buenws_backend.api.records.UserRecords;
import com.buenws.buenws_backend.api.service.InquiryService;
import com.buenws.buenws_backend.api.service.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/user")
public class UserController {

    private final InquiryService inquiryService;
    private final UserService userService;

    public UserController(InquiryService inquiryService, UserService userService) {
        this.inquiryService = inquiryService;
        this.userService = userService;
    }

    @PostMapping("/contact-submissions")
    public ResponseEntity<UserRecords.DefaultResponseRecord> submitContactForm(@RequestBody UserRecords.FormSubmissionRequestRecord formSubmissionRequestRecord) {
        return ResponseEntity.ok(inquiryService.submitContactForm(formSubmissionRequestRecord));
    }

    @PostMapping("/register")
    public ResponseEntity<UserRecords.RegisterResponseRecord> registerUser(@RequestBody UserRecords.CredentialsSubmitRequestRecord credentialsSubmitRequestRecord){
        return ResponseEntity.ok(userService.registerUser(credentialsSubmitRequestRecord));
    }

    @PostMapping("/login")
    public ResponseEntity<UserRecords.LoginResponseRecord> loginUser(@RequestBody UserRecords.CredentialsSubmitRequestRecord credentialsSubmitRequestRecord){
        return userService.loginUser(credentialsSubmitRequestRecord);
    }

    @PostMapping("/refresh-token")
    public ResponseEntity<UserRecords.RefreshTokenResponseRecord> refreshToken(@RequestBody UserRecords.RefreshTokenRequestRecord refreshTokenRequestRecord){
        return ResponseEntity.ok(userService.refreshToken(refreshTokenRequestRecord));
    }
}

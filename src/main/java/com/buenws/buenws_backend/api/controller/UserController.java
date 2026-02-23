package com.buenws.buenws_backend.api.controller;

import com.buenws.buenws_backend.api.records.UserRecords;
import com.buenws.buenws_backend.api.service.InquiryService;
import com.buenws.buenws_backend.api.service.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
public class UserController {

    private final InquiryService inquiryService;
    private final UserService userService;

    public UserController(InquiryService inquiryService, UserService userService) {
        this.inquiryService = inquiryService;
        this.userService = userService;
    }

    //Contact Form Submission
    @PostMapping("/contact-submissions")
    public ResponseEntity<UserRecords.ApiResponse> submitContactForm(@RequestBody UserRecords.FormSubmissionRequestRecord formSubmissionRequestRecord) {
        return ResponseEntity.ok(inquiryService.submitContactForm(formSubmissionRequestRecord));
    }

    //Auth Endpoints
    @GetMapping("/user/auth")
    public ResponseEntity<UserRecords.ApiResponse> checkAuth(){
        return ResponseEntity.ok(UserRecords.ApiResponse.success("Valid Authentication"));
    }

    @PostMapping("/user/auth/register")
    public ResponseEntity<UserRecords.ApiResponse> registerUser(@RequestBody UserRecords.CredentialsSubmitRequestRecord credentialsSubmitRequestRecord){
        return ResponseEntity.ok(userService.registerUser(credentialsSubmitRequestRecord));
    }

    @PostMapping("/user/auth/login")
    public ResponseEntity<UserRecords.ApiResponse> loginUser(@RequestBody UserRecords.CredentialsSubmitRequestRecord credentialsSubmitRequestRecord){
        return ResponseEntity.ok(userService.loginUser(credentialsSubmitRequestRecord));
    }

    @PostMapping("/user/auth/refresh")
    public ResponseEntity<UserRecords.ApiResponse> refreshToken(@RequestBody UserRecords.RefreshTokenRequestRecord refreshTokenRequestRecord){
        return ResponseEntity.ok(userService.refreshToken(refreshTokenRequestRecord));
    }
}

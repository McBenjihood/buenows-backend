package com.buenws.buenws_backend.api.user.controller;

import com.buenws.buenws_backend.api.user.records.UserRecords;
import com.buenws.buenws_backend.api.user.service.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/user")
public class UserController {

    public UserController(UserService userService) {
        this.userService = userService;
    }
    private UserService userService;

    @PostMapping("/contact-submissions")
    public ResponseEntity<UserRecords.FormSubmissionResponseRecord> submitContactForm(@RequestBody UserRecords.FormSubmissionRequestRecord formSubmissionRequestRecord) {
        return ResponseEntity.ok(new UserRecords.FormSubmissionResponseRecord(true, "Message sent successfully"));
    }

    @PostMapping("/register")
    public ResponseEntity<UserRecords.RegisterResponseRecord> registerUser(@RequestBody UserRecords.RegisterRequestRecord registerRequestRecord){
        return ResponseEntity.ok(new UserRecords.RegisterResponseRecord(true, "User registered successfully"));
    }
}

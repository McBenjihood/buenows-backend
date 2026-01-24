package com.buenws.buenws_backend.api.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.buenws.buenws_backend.api.records;

@RestController
@RequestMapping("/api/user")
public class UserController {

    @PostMapping("/contact-submissions")
    public ResponseEntity<records.FormSubmissionResponseRecord> submitContactForm(@RequestBody records.FormSubmissionResponseRecord formSubmissionRequestRecord) {
        return ResponseEntity.ok(new records.FormSubmissionResponseRecord(true, "Message sent successfully"));
    }
}

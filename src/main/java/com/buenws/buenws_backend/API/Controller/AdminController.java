package com.buenws.buenws_backend.API.Controller;

import com.buenws.buenws_backend.API.Records.Records;
import com.buenws.buenws_backend.API.Service.InquiryService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/admin")
public class AdminController {

    private final InquiryService inquiryService;

    public AdminController(InquiryService inquiryService) {
        this.inquiryService = inquiryService;
    }

    @GetMapping("/test")
    public ResponseEntity<Records.ApiResponse<String>> adminTest() {
        return ResponseEntity.ok(
                Records.ApiResponse.success("Admin access granted.", "ADMIN_OK")
        );
    }

    @GetMapping("/inquiries")
    public ResponseEntity<Records.ApiResponse<List<Records.InquiryResponse>>> getAllInquiries() {
        return ResponseEntity.ok(inquiryService.getAllInquiries());
    }
}
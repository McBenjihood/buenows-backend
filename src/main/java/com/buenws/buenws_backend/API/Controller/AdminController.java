package com.buenws.buenws_backend.API.Controller;

import com.buenws.buenws_backend.API.Records.Records;
import com.buenws.buenws_backend.API.Service.InquiryService;
import com.buenws.buenws_backend.API.Service.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/admin")
public class AdminController {

    private final InquiryService inquiryService;
    private final UserService userService;

    public AdminController(InquiryService inquiryService, UserService userService) {
        this.inquiryService = inquiryService;
        this.userService = userService;
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

    @GetMapping("/users")
    public ResponseEntity<Records.ApiResponse<List<Records.AdminUserResponse>>> getAllUsers() {
        return ResponseEntity.ok(userService.getAllUsersForAdmin());
    }
}
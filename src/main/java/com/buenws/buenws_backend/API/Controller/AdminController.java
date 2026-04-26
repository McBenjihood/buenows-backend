package com.buenws.buenws_backend.API.Controller;

import com.buenws.buenws_backend.API.Records.Records;
import com.buenws.buenws_backend.API.Service.InquiryService;
import com.buenws.buenws_backend.API.Service.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

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

    @DeleteMapping("/inquiries/{inquiryId}")
    public ResponseEntity<Records.ApiResponse<Void>> deleteInquiry(
            @PathVariable Long inquiryId
    ) {
        return ResponseEntity.ok(inquiryService.deleteInquiryForAdmin(inquiryId));
    }

    @GetMapping("/users")
    public ResponseEntity<Records.ApiResponse<List<Records.AdminUserResponse>>> getAllUsers() {
        return ResponseEntity.ok(userService.getAllUsersForAdmin());
    }

    @PutMapping("/users/{userId}/role")
    public ResponseEntity<Records.ApiResponse<Records.AdminUserResponse>> updateUserRole(
            @PathVariable UUID userId,
            @RequestBody Records.AdminUpdateRoleRequest request
    ) {
        return ResponseEntity.ok(userService.updateUserRole(userId, request));
    }

    @PutMapping("/users/{userId}/profile")
    public ResponseEntity<Records.ApiResponse<Records.AdminUserResponse>> updateUserProfile(
            @PathVariable UUID userId,
            @RequestBody Records.AdminUpdateUserProfileRequest request
    ) {
        return ResponseEntity.ok(userService.updateUserProfile(userId, request));
    }

    @DeleteMapping("/users/{userId}")
    public ResponseEntity<Records.ApiResponse<Void>> deleteUser(
            @PathVariable UUID userId
    ) {
        return ResponseEntity.ok(userService.deleteUserForAdmin(userId));
    }
}
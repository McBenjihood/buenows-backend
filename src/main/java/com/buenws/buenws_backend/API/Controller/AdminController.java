package com.buenws.buenws_backend.API.Controller;

import com.buenws.buenws_backend.API.Records.Records;
import com.buenws.buenws_backend.API.Service.InquiryService;
import com.buenws.buenws_backend.API.Service.UserService;
import com.buenws.buenws_backend.API.Service.RateLimitService;
import jakarta.servlet.http.HttpServletRequest;
import com.buenws.buenws_backend.Util.RequestUtil;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/admin")
public class AdminController {

    private final InquiryService inquiryService;
    private final UserService userService;
    private final RateLimitService rateLimitService;

    public AdminController(InquiryService inquiryService, UserService userService, RateLimitService rateLimitService) {
        this.inquiryService = inquiryService;
        this.userService = userService;
        this.rateLimitService = rateLimitService;
    }

    @GetMapping("/test")
    public ResponseEntity<Records.ApiResponse<String>> adminTest(HttpServletRequest request) {
        rateLimitService.checkBucket("test:" + RequestUtil.getClientIp(request), 1);
        return ResponseEntity.ok(
                Records.ApiResponse.success("Admin access granted.", "ADMIN_OK"));
    }

    @GetMapping("/inquiries")
    public ResponseEntity<Records.ApiResponse<Records.PageResponse<Records.InquiryResponse>>> getAllInquiries(
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "50") int size,
            HttpServletRequest request) {
        rateLimitService.checkBucket("inquiries/get:" + RequestUtil.getClientIp(request), 1);
        return ResponseEntity.ok(inquiryService.getAllInquiries(page, size));
    }

    @DeleteMapping("/inquiries/{inquiryId}")
    public ResponseEntity<Records.ApiResponse<Void>> deleteInquiryById(
            @PathVariable Long inquiryId,
            HttpServletRequest request) {
        rateLimitService.checkBucket("inquiries/delete:" + RequestUtil.getClientIp(request), 15);
        return ResponseEntity.ok(inquiryService.deleteInquiryForAdmin(inquiryId));
    }

    @GetMapping("/users")
    public ResponseEntity<Records.ApiResponse<Records.PageResponse<Records.AdminUserResponse>>> getAllUsers(
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "50") int size,
            HttpServletRequest request) {
        rateLimitService.checkBucket("users/get:" + RequestUtil.getClientIp(request), 1);
        return ResponseEntity.ok(userService.getAllUsersForAdmin(page, size));
    }

    @PutMapping("/users/update-role")
    public ResponseEntity<Records.ApiResponse<Records.AdminUserResponse>> updateUserRole(
            @Valid @RequestBody Records.AdminUpdateRoleRequest adminUpdateRoleRequest,
            HttpServletRequest servletRequest) {
        rateLimitService.checkBucket("users/update-role:" + RequestUtil.getClientIp(servletRequest), 15);
        return ResponseEntity.ok(userService.updateUserRole(adminUpdateRoleRequest));
    }

    @PutMapping("/users/update-profile")
    public ResponseEntity<Records.ApiResponse<Records.AdminUserResponse>> updateUserProfile(
            @Valid @RequestBody Records.AdminUpdateUserProfileRequest adminUpdateUserProfileRequest,
            HttpServletRequest servletRequest) {
        rateLimitService.checkBucket("users/update-profile:" + RequestUtil.getClientIp(servletRequest), 15);
        return ResponseEntity.ok(userService.updateUserProfile(adminUpdateUserProfileRequest));
    }

    @DeleteMapping("/users/{userId}")
    public ResponseEntity<Records.ApiResponse<Void>> deleteUserById(
            @PathVariable UUID userId,
            HttpServletRequest request) {
        rateLimitService.checkBucket("users/delete:" + RequestUtil.getClientIp(request), 20);
        return ResponseEntity.ok(userService.deleteUserForAdmin(userId));
    }
}

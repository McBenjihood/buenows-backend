package com.buenws.buenws_backend.API.Controller;

import com.buenws.buenws_backend.API.Records.Records;
import com.buenws.buenws_backend.API.Service.InquiryService;
import com.buenws.buenws_backend.API.Service.UserService;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Refill;
import jakarta.servlet.http.HttpServletRequest;
import com.buenws.buenws_backend.Util.RequestUtil;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@RestController
@RequestMapping("/api/admin")
public class AdminController {

    private final InquiryService inquiryService;
    private final UserService userService;

    private final Cache<String, Bucket> buckets = CacheBuilder.newBuilder()
            .expireAfterAccess(10, TimeUnit.MINUTES)
            .build();

    private Bucket getBucket(String key) {
        try {
            return buckets.get(key, () -> Bucket.builder()
                    .addLimit(Bandwidth.classic(60, Refill.intervally(60, Duration.ofMinutes(1))))
                    .build());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public AdminController(InquiryService inquiryService, UserService userService) {
        this.inquiryService = inquiryService;
        this.userService = userService;
    }

    @GetMapping("/test")
    public ResponseEntity<Records.ApiResponse<String>> adminTest(HttpServletRequest request) {
        String ipAddress = RequestUtil.getClientIp(request);
        if (getBucket("test:" + ipAddress).tryConsume(1)) {
            return ResponseEntity.ok(
                    Records.ApiResponse.success("Admin access granted.", "ADMIN_OK")
            );
        }
        return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).build();
    }

    @GetMapping("/inquiries")
    public ResponseEntity<Records.ApiResponse<List<Records.InquiryResponse>>> getAllInquiries(HttpServletRequest request) {
        String ipAddress = RequestUtil.getClientIp(request);
        if (getBucket("inquiries/get:" + ipAddress).tryConsume(1)) {
            return ResponseEntity.ok(inquiryService.getAllInquiries());
        }
        return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).build();
    }

    @DeleteMapping("/inquiries/{inquiryId}")
    public ResponseEntity<Records.ApiResponse<Void>> deleteInquiry(
            @PathVariable Long inquiryId,
            HttpServletRequest request
    ) {
        String ipAddress = RequestUtil.getClientIp(request);
        if (getBucket("inquiries/delete:" + ipAddress).tryConsume(15)) {
            return ResponseEntity.ok(inquiryService.deleteInquiryForAdmin(inquiryId));
        }
        return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).build();
    }

    @GetMapping("/users")
    public ResponseEntity<Records.ApiResponse<List<Records.AdminUserResponse>>> getAllUsers(HttpServletRequest request) {
        String ipAddress = RequestUtil.getClientIp(request);
        if (getBucket("users/get:" + ipAddress).tryConsume(1)) {
            return ResponseEntity.ok(userService.getAllUsersForAdmin());
        }
        return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).build();
    }

    @PutMapping("/users/{userId}/role")
    public ResponseEntity<Records.ApiResponse<Records.AdminUserResponse>> updateUserRole(
            @PathVariable UUID userId,
            @Valid @RequestBody Records.AdminUpdateRoleRequest request,
            HttpServletRequest servletRequest
    ) {
        String ipAddress = RequestUtil.getClientIp(servletRequest);
        if (getBucket("users/update-role:" + ipAddress).tryConsume(15)) {
            return ResponseEntity.ok(userService.updateUserRole(userId, request));
        }
        return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).build();
    }

    @PutMapping("/users/{userId}/profile")
    public ResponseEntity<Records.ApiResponse<Records.AdminUserResponse>> updateUserProfile(
            @PathVariable UUID userId,
            @Valid @RequestBody Records.AdminUpdateUserProfileRequest request,
            HttpServletRequest servletRequest
    ) {
        String ipAddress = RequestUtil.getClientIp(servletRequest);
        if (getBucket("users/update-profile:" + ipAddress).tryConsume(15)) {
            return ResponseEntity.ok(userService.updateUserProfile(userId, request));
        }
        return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).build();
    }

    @DeleteMapping("/users/{userId}")
    public ResponseEntity<Records.ApiResponse<Void>> deleteUser(
            @PathVariable UUID userId,
            HttpServletRequest request
    ) {
        String ipAddress = RequestUtil.getClientIp(request);
        if (getBucket("users/delete:" + ipAddress).tryConsume(20)) {
            return ResponseEntity.ok(userService.deleteUserForAdmin(userId));
        }
        return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).build();
    }
}
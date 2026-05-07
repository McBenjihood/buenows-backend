package com.buenws.buenws_backend.API.Controller;
 
import com.buenws.buenws_backend.API.Records.Records;
import com.buenws.buenws_backend.API.Service.InquiryService;
import com.buenws.buenws_backend.API.Service.UserService;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Refill;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
 
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@RestController
@RequestMapping("/api/admin")
public class AdminController {

    private final InquiryService inquiryService;
    private final UserService userService;

    private final Map<String, Bucket> buckets = new ConcurrentHashMap<>();

    private Bucket getBucket(String key) {
        return buckets.computeIfAbsent(key, k -> Bucket.builder()
                .addLimit(Bandwidth.classic(60, Refill.intervally(60, Duration.ofMinutes(1))))
                .build());
    }

    public AdminController(InquiryService inquiryService, UserService userService) {
        this.inquiryService = inquiryService;
        this.userService = userService;
    }

    @GetMapping("/test")
    public ResponseEntity<Records.ApiResponse<String>> adminTest() {
        if (getBucket("test").tryConsume(1)) {
            return ResponseEntity.ok(
                    Records.ApiResponse.success("Admin access granted.", "ADMIN_OK")
            );
        }
        return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).build();
    }

    @GetMapping("/inquiries")
    public ResponseEntity<Records.ApiResponse<List<Records.InquiryResponse>>> getAllInquiries() {
        if (getBucket("inquiries/get").tryConsume(1)) {
            return ResponseEntity.ok(inquiryService.getAllInquiries());
        }
        return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).build();
    }

    @DeleteMapping("/inquiries/{inquiryId}")
    public ResponseEntity<Records.ApiResponse<Void>> deleteInquiry(
            @PathVariable Long inquiryId
    ) {
        if (getBucket("inquiries/delete").tryConsume(15)) {
            return ResponseEntity.ok(inquiryService.deleteInquiryForAdmin(inquiryId));
        }
        return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).build();
    }

    @GetMapping("/users")
    public ResponseEntity<Records.ApiResponse<List<Records.AdminUserResponse>>> getAllUsers() {
        if (getBucket("users/get").tryConsume(1)) {
            return ResponseEntity.ok(userService.getAllUsersForAdmin());
        }
        return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).build();
    }

    @PutMapping("/users/{userId}/role")
    public ResponseEntity<Records.ApiResponse<Records.AdminUserResponse>> updateUserRole(
            @PathVariable UUID userId,
            @RequestBody Records.AdminUpdateRoleRequest request
    ) {
        if (getBucket("users/update-role").tryConsume(15)) {
            return ResponseEntity.ok(userService.updateUserRole(userId, request));
        }
        return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).build();
    }

    @PutMapping("/users/{userId}/profile")
    public ResponseEntity<Records.ApiResponse<Records.AdminUserResponse>> updateUserProfile(
            @PathVariable UUID userId,
            @RequestBody Records.AdminUpdateUserProfileRequest request
    ) {
        if (getBucket("users/update-profile").tryConsume(15)) {
            return ResponseEntity.ok(userService.updateUserProfile(userId, request));
        }
        return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).build();
    }

    @DeleteMapping("/users/{userId}")
    public ResponseEntity<Records.ApiResponse<Void>> deleteUser(
            @PathVariable UUID userId
    ) {
        if (getBucket("users/delete").tryConsume(20)) {
            return ResponseEntity.ok(userService.deleteUserForAdmin(userId));
        }
        return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).build();
    }
}
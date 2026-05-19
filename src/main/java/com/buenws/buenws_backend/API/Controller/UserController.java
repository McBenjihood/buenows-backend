package com.buenws.buenws_backend.API.Controller;

import com.buenws.buenws_backend.API.Records.Records;
import com.buenws.buenws_backend.API.Service.Tokens.TokenService;
import com.buenws.buenws_backend.API.Service.UserService;
import com.buenws.buenws_backend.Util.SecureCookieUtil;
import com.buenws.buenws_backend.Util.RequestUtil;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Refill;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.TimeUnit;

@RestController
@RequestMapping("/api/user")
public class UserController {


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

    public UserController(UserService userService) {
        this.userService = userService;
    }


    //Password changes
    @PostMapping("request-otp")
    public ResponseEntity<Records.ApiResponse<Void>> RequestOTP(@Valid @RequestBody Records.RequestOTPRequest requestOTPRequest, HttpServletRequest request){
        String ipAddress = RequestUtil.getClientIp(request);
        if (getBucket("request-otp:" + ipAddress ).tryConsume(10)) {
            return ResponseEntity.ok(userService.RequestOTP(requestOTPRequest));
        }
        return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).build();
    }

    @PostMapping("verify-otp")
    public ResponseEntity<Records.ApiResponse<Records.VerifyOTPResponse>> VerifyOTP(
            @Valid @RequestBody Records.VerifyOTPRequest verifyOTPRequest,
            HttpServletRequest request
    ){
        String ipAddress = RequestUtil.getClientIp(request);
        if (getBucket("verify-otp:" + ipAddress).tryConsume(5)) {
            return ResponseEntity.ok(userService.VerifyOTP(verifyOTPRequest));
        }
        return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).build();
    }

    @PostMapping("change-password")
    public ResponseEntity<Records.ApiResponse<Void>> ChangePassword(
            @Valid @RequestBody Records.ChangePasswordRequest changePasswordRequest,
            HttpServletRequest request
    ){
        String ipAddress = RequestUtil.getClientIp(request);
        if (getBucket("change-password:" + ipAddress).tryConsume(12)) {
            return ResponseEntity.ok(userService.ChangePassword(changePasswordRequest));
        }
        return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).build();
    }


    //Auth Endpoints
    @GetMapping("auth")
    public ResponseEntity<Records.ApiResponse<Records.AuthCheckResponse>> checkAuth(
            Authentication authentication,
            HttpServletRequest request
    ) {
        String ipAddress = RequestUtil.getClientIp(request);
        if (getBucket("auth:" + ipAddress).tryConsume(1)) {
            List<String> authorities = authentication.getAuthorities().stream()
                .map(Object::toString)
                .toList();

            return ResponseEntity.ok(
                Records.ApiResponse.success(
                        "Valid Authentication",
                        new Records.AuthCheckResponse(authentication.getName(), authorities)
                )
            );
        }
        return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).build();
    }

    @PostMapping("auth/register")
    public ResponseEntity<Records.ApiResponse<Records.SuccessfulAuthResponse>> registerUser(
            @Valid @RequestBody Records.RegisterCredentialsSubmitRequest credentialsSubmitRequest,
            HttpServletResponse response,
            HttpServletRequest request
    ) {
        String ipAddress = RequestUtil.getClientIp(request);
        if (getBucket("auth/register:" + ipAddress).tryConsume(20)) {
            Records.ApiResponse<Records.SuccessfulAuthResponse> authResponse =
                    userService.RegisterUserWithCredentials(credentialsSubmitRequest);

            SecureCookieUtil.addAuthCookies(response, authResponse.data());

            return ResponseEntity.ok(authResponse);
        }
        return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).build();
    }

    @PostMapping("auth/login")
    public ResponseEntity<Records.ApiResponse<Void>> loginUser(
            @Valid @RequestBody Records.CredentialsSubmitRequest credentialsSubmitRequest,
            HttpServletResponse response,
            HttpServletRequest request
    ) {
        String ipAddress = RequestUtil.getClientIp(request);
        if (getBucket("auth/login:" + ipAddress).tryConsume(10)) {
            Records.ApiResponse<Records.SuccessfulAuthResponse> authResponse =
                    userService.LoginUserWithCredentials(credentialsSubmitRequest);

            SecureCookieUtil.addAuthCookies(response, authResponse.data());

            return ResponseEntity.ok(Records.ApiResponse.success(authResponse.message()));
        }
        return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).build();
    }

    @PostMapping("auth/refresh")
    public ResponseEntity<Records.ApiResponse<Void>> refreshToken(
            HttpServletRequest request,
            HttpServletResponse response
    ) {
        String ipAddress = RequestUtil.getClientIp(request);
        if (getBucket("auth/refresh:" + ipAddress).tryConsume(5)) {
            String refreshToken = SecureCookieUtil.getTokenFromCookie(request, TokenService.REFRESH_TOKEN_COOKIE);

            Records.ApiResponse<Records.SuccessfulAuthResponse> authResponse =
                    userService.RefreshToken(refreshToken);

            SecureCookieUtil.addAuthCookies(response, authResponse.data());

            return ResponseEntity.ok(Records.ApiResponse.success(authResponse.message()));
        }
        return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).build();
    }

    @PostMapping("auth/logout")
    public ResponseEntity<Records.ApiResponse<Void>> logout(
            HttpServletRequest request,
            HttpServletResponse response
    ) {
        String ipAddress = RequestUtil.getClientIp(request);
        if (getBucket("auth/logout:" + ipAddress).tryConsume(1)) {
            String jwt = SecureCookieUtil.getTokenFromCookie(request, TokenService.ACCESS_TOKEN_COOKIE);

            Records.ApiResponse<Void> logoutResponse = userService.Logout(jwt);

            SecureCookieUtil.clearAuthCookies(response);

            return ResponseEntity.ok(logoutResponse);
        }
        return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).build();
    }
}
package com.buenws.buenws_backend.API.Controller;

import com.buenws.buenws_backend.API.Records.Records;
import com.buenws.buenws_backend.API.Service.RateLimitService;
import com.buenws.buenws_backend.API.Service.Tokens.TokenService;
import com.buenws.buenws_backend.API.Service.UserService;
import com.buenws.buenws_backend.Util.SecureCookieUtil;
import com.buenws.buenws_backend.Util.RequestUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/user")
public class UserController {

    private final UserService userService;
    private final RateLimitService rateLimitService;

    public UserController(UserService userService, RateLimitService rateLimitService) {
        this.userService = userService;
        this.rateLimitService = rateLimitService;
    }

    //OTP Endpoints
    @PostMapping("request-otp")
    public ResponseEntity<Records.ApiResponse<Void>> RequestOTP(@Valid @RequestBody Records.RequestOTPRequest requestOTPRequest, HttpServletRequest request) {
        rateLimitService.checkBucket("request-otp:" + RequestUtil.getClientIp(request), 10);
        return ResponseEntity.ok(userService.RequestOTP(requestOTPRequest));
    }

    @PostMapping("verify-otp")
    public ResponseEntity<Records.ApiResponse<Records.VerifyOTPResponse>> VerifyOTP(
            @Valid @RequestBody Records.VerifyOTPRequest verifyOTPRequest,
            HttpServletRequest request
    ) {
        rateLimitService.checkBucket("verify-otp:" + RequestUtil.getClientIp(request), 5);
        return ResponseEntity.ok(userService.VerifyOTP(verifyOTPRequest));
    }



    @PostMapping("change-password")
    public ResponseEntity<Records.ApiResponse<Void>> ChangePassword(
            @Valid @RequestBody Records.ChangePasswordRequest changePasswordRequest,
            HttpServletRequest request
    ) {
        rateLimitService.checkBucket("change-password:" + RequestUtil.getClientIp(request), 12);
        return ResponseEntity.ok(userService.ChangePassword(changePasswordRequest));
    }

    //Auth Endpoints
    @GetMapping("auth")
    public ResponseEntity<Records.ApiResponse<Records.AuthCheckResponse>> checkAuth(
            Authentication authentication,
            HttpServletRequest request
    ) {
        rateLimitService.checkBucket("auth:" + RequestUtil.getClientIp(request), 1);

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

    @PostMapping("auth/register")
    public ResponseEntity<Records.ApiResponse<Records.SuccessfulAuthResponse>> registerUser(
            @Valid @RequestBody Records.RegisterCredentialsSubmitRequest credentialsSubmitRequest,
            HttpServletResponse response,
            HttpServletRequest request
    ) {
        rateLimitService.checkBucket("auth/register:" + RequestUtil.getClientIp(request), 20);

        Records.ApiResponse<Records.SuccessfulAuthResponse> authResponse =
                userService.RegisterUserWithCredentials(credentialsSubmitRequest);

        SecureCookieUtil.addAuthCookies(response, authResponse.data());

        return ResponseEntity.ok(authResponse);
    }

    @PostMapping("auth/login")
    public ResponseEntity<Records.ApiResponse<Records.SuccessfulAuthResponse>> loginUser(
            @Valid @RequestBody Records.CredentialsSubmitRequest credentialsSubmitRequest,
            HttpServletResponse response,
            HttpServletRequest request
    ) {
        rateLimitService.checkBucket("auth/login:" + RequestUtil.getClientIp(request), 10);

        Records.ApiResponse<Records.SuccessfulAuthResponse> authResponse =
                userService.LoginUserWithCredentials(credentialsSubmitRequest);

        SecureCookieUtil.addAuthCookies(response, authResponse.data());

        return ResponseEntity.ok(authResponse);
    }

    @PostMapping("auth/refresh")
    public ResponseEntity<Records.ApiResponse<Records.SuccessfulAuthResponse>> refreshToken(
            HttpServletRequest request,
            HttpServletResponse response
    ) {
        rateLimitService.checkBucket("auth/refresh:" + RequestUtil.getClientIp(request), 5);

        String refreshToken = SecureCookieUtil.getTokenFromCookie(request, TokenService.REFRESH_TOKEN_COOKIE);

        Records.ApiResponse<Records.SuccessfulAuthResponse> authResponse =
                userService.RefreshToken(refreshToken);

        SecureCookieUtil.addAuthCookies(response, authResponse.data());

        return ResponseEntity.ok(authResponse);
    }

    @PostMapping("auth/logout")
    public ResponseEntity<Records.ApiResponse<Void>> logout(
            HttpServletRequest request,
            HttpServletResponse response
    ) {
        rateLimitService.checkBucket("auth/logout:" + RequestUtil.getClientIp(request), 1);

        String refreshToken = SecureCookieUtil.getTokenFromCookie(request, TokenService.REFRESH_TOKEN_COOKIE);

        Records.ApiResponse<Void> logoutResponse = userService.Logout(refreshToken);

        SecureCookieUtil.clearAuthCookies(response);

        return ResponseEntity.ok(logoutResponse);
    }
}

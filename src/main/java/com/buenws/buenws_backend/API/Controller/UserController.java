package com.buenws.buenws_backend.API.Controller;

import com.buenws.buenws_backend.API.Records.Records;
import com.buenws.buenws_backend.API.Service.Tokens.TokenService;
import com.buenws.buenws_backend.API.Service.UserService;
import com.buenws.buenws_backend.Util.SecureCookieUtil;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Refill;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@RestController
@RequestMapping("/api/user")
public class UserController {


    private final UserService userService;

    private final Map<String, Bucket> buckets = new ConcurrentHashMap<>();

    private Bucket getBucket(String key) {
        return buckets.computeIfAbsent(key, k -> Bucket.builder()
                .addLimit(Bandwidth.classic(60, Refill.intervally(60, Duration.ofMinutes(1))))
                .build());
    }

    public UserController(UserService userService) {
        this.userService = userService;
    }


    //Password changes
    @PostMapping("request-otp")
    public ResponseEntity<Records.ApiResponse<Void>> InitChangePassword(@RequestBody Records.InitResetPasswordRequest initResetPasswordRequest){
        if (getBucket("request-otp").tryConsume(10)) {
            return ResponseEntity.ok(userService.InitChangePassword(initResetPasswordRequest));
        }
        return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).build();
    }

    @PostMapping("verify-otp")
    public ResponseEntity<Records.ApiResponse<Records.VerifyOTPResponse>> VerifyOTP(@RequestBody Records.VerifyOTPRequest verifyOTPRequest){
        if (getBucket("verify-otp").tryConsume(5)) {
            return ResponseEntity.ok(userService.VerifyOTP(verifyOTPRequest));
        }
        return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).build();
    }

    @PostMapping("change-password")
    public ResponseEntity<Records.ApiResponse<Void>> ChangePassword(@RequestBody Records.ChangePasswordRequest changePasswordRequest){
        if (getBucket("change-password").tryConsume(12)) {
            return ResponseEntity.ok(userService.ChangePassword(changePasswordRequest));
        }
        return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).build();

    }

    //Auth Endpoints
    @GetMapping("auth")
    public ResponseEntity<Records.ApiResponse<Records.AuthCheckResponse>> checkAuth(Authentication authentication) {
        if (getBucket("auth").tryConsume(1)) {
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
            @RequestBody Records.CredentialsSubmitRequest credentialsSubmitRequest,
            HttpServletResponse response
    ) {
        if (getBucket("auth/register").tryConsume(20)) {
            Records.ApiResponse<Records.SuccessfulAuthResponse> authResponse =
                    userService.RegisterUserWithCredentials(credentialsSubmitRequest);

            SecureCookieUtil.addAuthCookies(response, authResponse.data());

            return ResponseEntity.ok(authResponse);
        }
        return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).build();
    }

    @PostMapping("auth/login")
    public ResponseEntity<Records.ApiResponse<Void>> loginUser(
            @RequestBody Records.CredentialsSubmitRequest credentialsSubmitRequest,
            HttpServletResponse response
    ) {
        if (getBucket("auth/login").tryConsume(10)) {
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
        if (getBucket("auth/refresh").tryConsume(5)) {
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
        if (getBucket("auth/logout").tryConsume(1)) {
            String refreshToken = SecureCookieUtil.getTokenFromCookie(request, TokenService.REFRESH_TOKEN_COOKIE);

            Records.ApiResponse<Void> logoutResponse = userService.Logout(refreshToken);

            SecureCookieUtil.clearAuthCookies(response);

            return ResponseEntity.ok(logoutResponse);
        }
        return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).build();
    }
}
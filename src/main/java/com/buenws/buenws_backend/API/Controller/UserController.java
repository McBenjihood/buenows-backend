package com.buenws.buenws_backend.API.Controller;

import com.buenws.buenws_backend.API.Records.Records;
import com.buenws.buenws_backend.API.Service.Tokens.TokenService;
import com.buenws.buenws_backend.API.Service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;
import java.util.List;

@RestController
@RequestMapping("/api/user")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    //Password changes
    @PostMapping("request-otp")
    public ResponseEntity<Records.ApiResponse<Void>> InitChangePassword(@RequestBody Records.InitResetPasswordRequest initResetPasswordRequest){
        return ResponseEntity.ok(userService.InitChangePassword(initResetPasswordRequest));
    }

    @PostMapping("verify-otp")
    public ResponseEntity<Records.ApiResponse<Records.VerifyOTPResponse>> VerifyOTP(@RequestBody Records.VerifyOTPRequest verifyOTPRequest){
        return ResponseEntity.ok(userService.VerifyOTP(verifyOTPRequest));
    }

    @PostMapping("change-password")
    public ResponseEntity<Records.ApiResponse<Void>> ChangePassword(@RequestBody Records.ChangePasswordRequest changePasswordRequest){
        return ResponseEntity.ok(userService.ChangePassword(changePasswordRequest));
    }


    //Auth Endpoints
    @GetMapping("auth")
    public ResponseEntity<Records.ApiResponse<Records.AuthCheckResponse>> checkAuth(Authentication authentication) {
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
    public ResponseEntity<Records.ApiResponse<Void>> registerUser(
            @RequestBody Records.CredentialsSubmitRequest credentialsSubmitRequest,
            HttpServletResponse response
    ) {
        Records.ApiResponse<Records.SuccessfulAuthResponse> authResponse =
                userService.RegisterUserWithCredentials(credentialsSubmitRequest);

        addAuthCookies(response, authResponse.data());

        return ResponseEntity.ok(Records.ApiResponse.success(authResponse.message()));
    }

    @PostMapping("auth/login")
    public ResponseEntity<Records.ApiResponse<Void>> loginUser(
            @RequestBody Records.CredentialsSubmitRequest credentialsSubmitRequest,
            HttpServletResponse response
    ) {
        Records.ApiResponse<Records.SuccessfulAuthResponse> authResponse =
                userService.LoginUserWithCredentials(credentialsSubmitRequest);

        addAuthCookies(response, authResponse.data());

        return ResponseEntity.ok(Records.ApiResponse.success(authResponse.message()));
    }

    @PostMapping("auth/refresh")
    public ResponseEntity<Records.ApiResponse<Void>> refreshToken(
            HttpServletRequest request,
            HttpServletResponse response
    ) {
        String refreshToken = getTokenFromCookie(request, TokenService.REFRESH_TOKEN_COOKIE);

        Records.ApiResponse<Records.SuccessfulAuthResponse> authResponse =
                userService.RefreshToken(refreshToken);

        addAuthCookies(response, authResponse.data());

        return ResponseEntity.ok(Records.ApiResponse.success(authResponse.message()));
    }

    @PostMapping("auth/logout")
    public ResponseEntity<Records.ApiResponse<Void>> logout(
            HttpServletRequest request,
            HttpServletResponse response
    ) {
        String refreshToken = getTokenFromCookie(request, TokenService.REFRESH_TOKEN_COOKIE);

        Records.ApiResponse<Void> logoutResponse = userService.Logout(refreshToken);

        clearAuthCookies(response);

        return ResponseEntity.ok(logoutResponse);
    }

    private void addAuthCookies(HttpServletResponse response, Records.SuccessfulAuthResponse authResponse) {
        ResponseCookie accessCookie = ResponseCookie.from(TokenService.ACCESS_TOKEN_COOKIE, authResponse.JWT())
                .httpOnly(true)
                .secure(false)
                .sameSite("Lax")
                .path("/")
                .maxAge(Duration.ofHours(1))
                .build();

        ResponseCookie refreshCookie = ResponseCookie.from(TokenService.REFRESH_TOKEN_COOKIE, authResponse.RefreshToken())
                .httpOnly(true)
                .secure(false)
                .sameSite("Lax")
                .path("/api/user/auth")
                .maxAge(Duration.ofDays(7))
                .build();

        response.addHeader(HttpHeaders.SET_COOKIE, accessCookie.toString());
        response.addHeader(HttpHeaders.SET_COOKIE, refreshCookie.toString());
    }

    private void clearAuthCookies(HttpServletResponse response) {
        ResponseCookie accessCookie = ResponseCookie.from(TokenService.ACCESS_TOKEN_COOKIE, "")
                .httpOnly(true)
                .secure(false)
                .sameSite("Lax")
                .path("/")
                .maxAge(0)
                .build();

        ResponseCookie refreshCookie = ResponseCookie.from(TokenService.REFRESH_TOKEN_COOKIE, "")
                .httpOnly(true)
                .secure(false)
                .sameSite("Lax")
                .path("/api/user/auth")
                .maxAge(0)
                .build();

        response.addHeader(HttpHeaders.SET_COOKIE, accessCookie.toString());
        response.addHeader(HttpHeaders.SET_COOKIE, refreshCookie.toString());
    }

    private String getTokenFromCookie(HttpServletRequest request, String cookieName) {
        if (request.getCookies() == null) {
            return null;
        }

        for (jakarta.servlet.http.Cookie cookie : request.getCookies()) {
            if (cookieName.equals(cookie.getName())) {
                return cookie.getValue();
            }
        }

        return null;
    }
}
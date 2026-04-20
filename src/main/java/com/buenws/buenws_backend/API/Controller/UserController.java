package com.buenws.buenws_backend.API.Controller;

import com.buenws.buenws_backend.API.Records.UserRecords;
import com.buenws.buenws_backend.API.Service.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/user")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("reset-password")
    public ResponseEntity<UserRecords.ApiResponse<Void>> resetPassword(@RequestBody UserRecords.ResetPasswordRequest resetPasswordRequest){
        return ResponseEntity.ok(userService.ChangePassword(resetPasswordRequest));
    }

    //Auth Endpoints
    @GetMapping("auth")
    public ResponseEntity<UserRecords.ApiResponse<Void>> checkAuth(){
        return ResponseEntity.ok(UserRecords.ApiResponse.success("Valid Authentication"));
    }

    @PostMapping("auth/register")
    public ResponseEntity<UserRecords.ApiResponse<UserRecords.SuccessfulAuthResponse>> registerUser(@RequestBody UserRecords.CredentialsSubmitRequest credentialsSubmitRequest){
        return ResponseEntity.ok(userService.RegisterUserWithCredentials(credentialsSubmitRequest));
    }

    @PostMapping("auth/login")
    public ResponseEntity<UserRecords.ApiResponse<UserRecords.SuccessfulAuthResponse>> loginUser(@RequestBody UserRecords.CredentialsSubmitRequest credentialsSubmitRequest){
        return ResponseEntity.ok(userService.LoginUserWithCredentials(credentialsSubmitRequest));
    }

    @PostMapping("auth/refresh")
    public ResponseEntity<UserRecords.ApiResponse<UserRecords.SuccessfulAuthResponse>> refreshToken(@RequestBody UserRecords.RefreshTokenRequest refreshTokenRequest){
        return ResponseEntity.ok(userService.RefreshToken(refreshTokenRequest));
    }
}

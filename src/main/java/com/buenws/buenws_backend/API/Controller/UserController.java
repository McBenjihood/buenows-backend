package com.buenws.buenws_backend.API.Controller;

import com.buenws.buenws_backend.API.Records.Records;
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

    @PostMapping("request-reset-password")
    public ResponseEntity<Records.ApiResponse<Void>> InitChangePassword(@RequestBody Records.InitResetPasswordRequest initResetPasswordRequest){
        return ResponseEntity.ok(userService.InitChangePassword(initResetPasswordRequest));
    }


    @PostMapping("reset-password")
    public ResponseEntity<Records.ApiResponse<Void>> VerifyOTP(@RequestBody Records.VerifyOTPResetPasswordRequest verifyOTPResetPasswordRequest){
        return ResponseEntity.ok(userService.VerifyOTPChangePassword(verifyOTPResetPasswordRequest));
    }


    //Auth Endpoints
    @GetMapping("auth")
    public ResponseEntity<Records.ApiResponse<Void>> checkAuth(){
        return ResponseEntity.ok(Records.ApiResponse.success("Valid Authentication"));
    }

    @PostMapping("auth/register")
    public ResponseEntity<Records.ApiResponse<Records.SuccessfulAuthResponse>> registerUser(@RequestBody Records.CredentialsSubmitRequest credentialsSubmitRequest){
        return ResponseEntity.ok(userService.RegisterUserWithCredentials(credentialsSubmitRequest));
    }

    @PostMapping("auth/login")
    public ResponseEntity<Records.ApiResponse<Records.SuccessfulAuthResponse>> loginUser(@RequestBody Records.CredentialsSubmitRequest credentialsSubmitRequest){
        return ResponseEntity.ok(userService.LoginUserWithCredentials(credentialsSubmitRequest));
    }

    @PostMapping("auth/refresh")
    public ResponseEntity<Records.ApiResponse<Records.SuccessfulAuthResponse>> refreshToken(@RequestBody Records.RefreshTokenRequest refreshTokenRequest){
        return ResponseEntity.ok(userService.RefreshToken(refreshTokenRequest));
    }
}

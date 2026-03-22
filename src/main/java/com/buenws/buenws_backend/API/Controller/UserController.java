package com.buenws.buenws_backend.API.Controller;

import com.buenws.buenws_backend.API.Records.UserRecords;
import com.buenws.buenws_backend.API.Service.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.core.Authentication;

@RestController
@RequestMapping("/api/user")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    //Auth Endpoints
    @GetMapping("auth")
    public ResponseEntity<UserRecords.ApiResponse<Void>> checkAuth(){
        return ResponseEntity.ok(UserRecords.ApiResponse.success("Valid Authentication"));
    }

    @GetMapping("/me")
    public ResponseEntity<UserRecords.ApiResponse<UserRecords.UserProfileResponseRecord>> getCurrentUser(Authentication authentication) {
        return ResponseEntity.ok(userService.getCurrentUser(authentication.getName()));
    }

    @PostMapping("auth/register")
    public ResponseEntity<UserRecords.ApiResponse<Void>> registerUser(@RequestBody UserRecords.CredentialsSubmitRequestRecord credentialsSubmitRequestRecord){
        return ResponseEntity.ok(userService.registerUser(credentialsSubmitRequestRecord));
    }

    @PostMapping("auth/login")
    public ResponseEntity<UserRecords.ApiResponse<UserRecords.LoginResponseRecord>> loginUser(@RequestBody UserRecords.CredentialsSubmitRequestRecord credentialsSubmitRequestRecord){
        return ResponseEntity.ok(userService.loginUser(credentialsSubmitRequestRecord));
    }

    @PostMapping("auth/refresh")
    public ResponseEntity<UserRecords.ApiResponse<UserRecords.RefreshTokenResponseRecord>> refreshToken(@RequestBody UserRecords.RefreshTokenRequestRecord refreshTokenRequestRecord){
        return ResponseEntity.ok(userService.refreshToken(refreshTokenRequestRecord));
    }
}

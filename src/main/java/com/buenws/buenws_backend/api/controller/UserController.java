package com.buenws.buenws_backend.api.controller;

import com.buenws.buenws_backend.api.records.UserRecords;
import com.buenws.buenws_backend.api.service.InquiryService;
import com.buenws.buenws_backend.api.service.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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

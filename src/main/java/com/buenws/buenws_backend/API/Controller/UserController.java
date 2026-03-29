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

    //Auth Endpoints
    @GetMapping("auth")
    public ResponseEntity<UserRecords.ApiResponse<Void>> checkAuth(){
        return ResponseEntity.ok(UserRecords.ApiResponse.success("Valid Authentication"));
    }

    @PostMapping("auth/register")
    public ResponseEntity<UserRecords.ApiResponse<UserRecords.SuccessfulAuthResponseRecord>> registerUser(@RequestBody UserRecords.CredentialsSubmitRequestRecord credentialsSubmitRequestRecord){
        return ResponseEntity.ok(userService.RegisterUserWithCredentials(credentialsSubmitRequestRecord));
    }

    @PostMapping("auth/login")
    public ResponseEntity<UserRecords.ApiResponse<UserRecords.SuccessfulAuthResponseRecord>> loginUser(@RequestBody UserRecords.CredentialsSubmitRequestRecord credentialsSubmitRequestRecord){
        return ResponseEntity.ok(userService.LoginUserWithCredentials(credentialsSubmitRequestRecord));
    }

    @PostMapping("auth/refresh")
    public ResponseEntity<UserRecords.ApiResponse<UserRecords.SuccessfulAuthResponseRecord>> refreshToken(@RequestBody UserRecords.RefreshTokenRequestRecord refreshTokenRequestRecord){
        return ResponseEntity.ok(userService.RefreshTokens(refreshTokenRequestRecord));
    }
}

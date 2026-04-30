package com.buenws.buenws_backend.API.Controller;

import com.buenws.buenws_backend.API.Records.Records;
import com.buenws.buenws_backend.API.Service.Tokens.TokenService;
import com.buenws.buenws_backend.API.Service.UserAssetService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;


@RestController
@RequestMapping("/api/files")
public class UserAssetController {
/*
    private final UserAssetService userAssetService;
    private final TokenService tokenService;

    public UserAssetController(UserAssetService userAssetService, TokenService tokenService) {
        this.userAssetService = userAssetService;
        this.tokenService = tokenService;
    }

    @PostMapping("/upload")
    public ResponseEntity<Records.ApiResponse<Records.UploadFileResponse>> handleFileUpload(
            @RequestParam("file") MultipartFile file,
            @RequestHeader("Authorization") String authHeader
    ) {
        return ResponseEntity.ok(userAssetService.handleImageUpload(file, authHeader));
    }

    @GetMapping("/get-files")
    public ResponseEntity<Records.ApiResponse<List<String>>> getImageList(
            @RequestHeader("Authorization") String authHeader
    ) {
        return ResponseEntity.ok(userAssetService.getImageList(tokenService.parseTokenFromHeader(authHeader)));
    }
 */
}
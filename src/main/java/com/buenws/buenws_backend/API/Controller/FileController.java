package com.buenws.buenws_backend.API.Controller;

import com.buenws.buenws_backend.API.Records.UserRecords;
import com.buenws.buenws_backend.API.Service.UserAssetService;
import com.buenws.buenws_backend.API.Service.Tokens.TokenService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;


@RestController
@RequestMapping("/api/files")
public class FileController {

    public FileController(UserAssetService fileservice, TokenService tokenService) {
        this.fileservice = fileservice;
        this.tokenService = tokenService;
    }

    private final UserAssetService fileservice;
    private final TokenService tokenService;


    @PostMapping("upload")
    ResponseEntity<UserRecords.ApiResponse<Void>> handleFileUpload(@RequestParam("file")MultipartFile file, @RequestHeader("Authorization") String authHeader){
        return ResponseEntity.ok(fileservice.handleFileUpload(file, tokenService.parseTokenFromHeader(authHeader)));
    }

    /*
    @GetMapping("get-files")
    ResponseEntity<UserRecords.ApiResponse<Void>> getImageList(@RequestHeader("Authorization") String authHeader){
        return ResponseEntity.ok(fileservice.);
    }
     */
}

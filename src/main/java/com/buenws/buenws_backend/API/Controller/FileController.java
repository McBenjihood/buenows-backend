package com.buenws.buenws_backend.API.Controller;

import com.buenws.buenws_backend.API.Records.UserRecords;
import com.buenws.buenws_backend.API.Service.FileService;
import com.buenws.buenws_backend.API.Service.Tokens.TokenService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/files")
public class FileController {

    private final FileService fileService;
    private final TokenService tokenService;

    public FileController(FileService fileService, TokenService tokenService) {
        this.fileService = fileService;
        this.tokenService = tokenService;
    }

    @PostMapping("/upload")
    public ResponseEntity<UserRecords.ApiResponse<Void>> handleFileUpload(
            @RequestParam("file") MultipartFile file,
            @RequestHeader("Authorization") String authHeader
    ) {
        String token = tokenService.parseTokenFromHeader(authHeader);
        return ResponseEntity.ok(fileService.handleFileUpload(file, token));
    }

    @GetMapping("/get-files")
    public ResponseEntity<UserRecords.ApiResponse<List<String>>> getImageList(
            @RequestHeader("Authorization") String authHeader
    ) {
        String token = tokenService.parseTokenFromHeader(authHeader);
        return ResponseEntity.ok(fileService.getImageList(token));
    }
}
package com.buenws.buenws_backend.API.Controller;

import com.buenws.buenws_backend.API.Records.UserRecords;
import com.buenws.buenws_backend.API.Service.FileService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;


@RestController
@RequestMapping("/api/files")
public class FileController {

    private FileService fileservice;

    private static final String UPLOAD_DIR = "uploads/";

    public FileController(FileService fileservice) {
        this.fileservice = fileservice;
    }

    @PostMapping("upload")
    ResponseEntity<UserRecords.ApiResponse<Void>> handleFileUpload(@RequestParam("file")MultipartFile file, @RequestHeader("Authorization") String authHeader){
        return ResponseEntity.ok(fileservice.handleFileUpload(file, UPLOAD_DIR, authHeader));
    }
}

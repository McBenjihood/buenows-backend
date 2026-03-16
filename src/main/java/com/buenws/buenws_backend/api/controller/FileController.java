package com.buenws.buenws_backend.api.controller;

import com.buenws.buenws_backend.api.records.UserRecords;
import com.buenws.buenws_backend.api.service.FileService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;


@RestController
@RequestMapping("/api/files")
public class FileController {

    private FileService fileservice;
    private static final String UPLOAD_DIR = "uploads/";


    @PostMapping
    ResponseEntity<UserRecords.ApiResponse<Void>> handleFileUpload(@RequestParam("file")MultipartFile file){
        return ResponseEntity.ok(fileservice.handleFileUpload(file, UPLOAD_DIR));
    }
}

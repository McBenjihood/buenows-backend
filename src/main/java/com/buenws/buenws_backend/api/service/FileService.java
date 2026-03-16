package com.buenws.buenws_backend.api.service;

import com.buenws.buenws_backend.api.exception.customExceptions.InvalidFileOperation;
import com.buenws.buenws_backend.api.records.UserRecords;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Objects;

@Service
public class FileService {
    public UserRecords.ApiResponse<Void> handleFileUpload (MultipartFile file, String UPLOAD_DIR){
        try{
            Path uploadPath = Paths.get(UPLOAD_DIR);

            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath);
            }

            Path filePath = uploadPath.resolve(Objects.requireNonNull(file.getOriginalFilename()));
            file.transferTo(filePath.toFile());

            return UserRecords.ApiResponse.success("File uploaded: " + filePath.getFileName());
        } catch (IOException e) {
            throw new InvalidFileOperation("File Upload failed. Try renaming the file.","INVALID_FILE_UPLOAD", e);
        }
    }
}

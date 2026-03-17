package com.buenws.buenws_backend.api.service;

import com.buenws.buenws_backend.api.entity.UserEntity;
import com.buenws.buenws_backend.api.exception.customExceptions.InvalidFileOperation;
import com.buenws.buenws_backend.api.exception.customExceptions.InvalidUserException;
import com.buenws.buenws_backend.api.records.UserRecords;
import com.buenws.buenws_backend.api.service.tokens.TokenService;
import com.buenws.buenws_backend.util.BuenowsUtil;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Objects;
import java.util.Optional;

@Service
public class FileService {

    TokenService tokenService;

    public FileService(TokenService tokenService) {
        this.tokenService = tokenService;
    }

    private void listDirContent(){

    }

    public UserRecords.ApiResponse<Void> handleFileUpload (MultipartFile file, String UPLOAD_DIR, String authHeader){
        try{
            Optional<UserEntity> userEntity = tokenService.validateJWTToken(tokenService.parseTokenFromHeader(authHeader));

            if(userEntity.isPresent()){

                UserEntity user = userEntity.get();

                Path uploadPath = Paths.get(UPLOAD_DIR + user.getId().toString() + "/");

                if (!Files.exists(uploadPath)) {
                    Files.createDirectories(uploadPath);
                }

                Path filePath = uploadPath.resolve(BuenowsUtil.getFilePrefix() +"_image.jpg");
                file.transferTo(filePath);

                return UserRecords.ApiResponse.success("File uploaded: " + filePath.getFileName());
            }else {
                throw new InvalidUserException("This user is not permitted to take this action.", "INVALID_USER");
            }
        } catch (IOException e) {
            throw new InvalidFileOperation("File Upload failed. Try renaming the file.","INVALID_FILE_UPLOAD", e);
        }
    }
}

package com.buenws.buenws_backend.API.Service;

import com.buenws.buenws_backend.API.Entity.UserEntity;
import com.buenws.buenws_backend.API.Exception.Custom.InvalidFileOperation;
import com.buenws.buenws_backend.API.Records.UserRecords;
import com.buenws.buenws_backend.API.Service.Tokens.TokenService;
import com.buenws.buenws_backend.Util.FileUtil;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class FileService {

    private final TokenService tokenService;
    private final UserService userService;
    private final String UPLOAD_DIR = "src/main/resources/static/images/";


    public FileService(UserService userService, TokenService tokenService) {
        this.userService = userService;
        this.tokenService = tokenService;
    }

    public List<String> listDirContent(String directoryPath) {
        File directory = new File(directoryPath);
        File[] files = directory.listFiles();

        if (files == null) {
            return Collections.emptyList();
        }

        return Arrays.stream(files)
                .map(File::getName)
                .collect(Collectors.toList());
    }

    public UserRecords.ApiResponse<Void> handleFileUpload (MultipartFile file, String token){
        try{
                UserEntity user = userService.getUserEntityFromToken(token);
                Path uploadPath = Paths.get(UPLOAD_DIR + user.getId().toString() + "/");

                if (!Files.exists(uploadPath)) {
                    Files.createDirectories(uploadPath);
                }

                Path filePath = uploadPath.resolve(FileUtil.getFilePrefix() +"_image.jpg");
                file.transferTo(filePath);

                return UserRecords.ApiResponse.success("File uploaded: " + filePath.getFileName());
        } catch (IOException e) {
            throw new InvalidFileOperation("File Upload failed. Try renaming the file.","INVALID_FILE_UPLOAD", e);
        }
    }

    public UserRecords.ApiResponse<Void> getImageList(String token){
        try {
            UserEntity user = userService.getUserEntityFromToken(token);
            List<String> images = listDirContent(UPLOAD_DIR + user.getId().toString());


        }
    }
}

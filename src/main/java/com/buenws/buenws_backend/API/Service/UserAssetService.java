package com.buenws.buenws_backend.API.Service;

import com.buenws.buenws_backend.API.Entity.UserAssetEntity;
import com.buenws.buenws_backend.API.Entity.UserEntity;
import com.buenws.buenws_backend.API.Exception.Custom.InvalidFileOperation;
import com.buenws.buenws_backend.API.Records.Records;
import com.buenws.buenws_backend.API.Repository.UserAssetsRepository;
import com.buenws.buenws_backend.API.Service.Tokens.TokenService;
import com.buenws.buenws_backend.Util.FileUtil;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

@Service
public class UserAssetService {

    private final TokenService tokenService;
    private final UserService userService;
    private final UserAssetsRepository userAssetsRepository;

    private final String UPLOAD_DIR = "src/main/resources/static/images/";

    public UserAssetService(TokenService tokenService, UserService userService, UserAssetsRepository userAssetsRepository) {
        this.tokenService = tokenService;
        this.userService = userService;
        this.userAssetsRepository = userAssetsRepository;
    }

    @Transactional
    public Records.ApiResponse<Records.UploadFileResponse> handleImageUpload(MultipartFile file, String Token) {
        try {
            UserEntity user = userService.getUserEntityFromToken(Token);
            Path uploadPath = Paths.get(UPLOAD_DIR + user.getId() + "/");

            //creating a directory to store images
            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath);
            }

            String FilePrefix = FileUtil.getFilePrefix();

            Path FileDirectory = Paths.get(uploadPath + "/"+ FilePrefix);
            Path TempFilePath = Paths.get(uploadPath + "/"+ FilePrefix + "_image.tmp");

            //writing temporary file to drive
            file.transferTo(TempFilePath);

            //Setting IO for ImageIO.write()
            BufferedImage InputImage = ImageIO.read(new File(TempFilePath.toString()));
            File OutputFile = new File(FileDirectory + "_image.png");

            //Converting image into png file
            if(!ImageIO.write(InputImage, "png" , OutputFile)){
                throw new IOException();
            }

            //Delete tmp file after successful conversion
            Files.delete(TempFilePath);

            UserAssetEntity userAssetEntity = new UserAssetEntity("image", "http://localhost:8080/images/" + user.getId() + "/"+ FilePrefix + "_image.png", OutputFile.getPath(), user);
            userAssetsRepository.save(userAssetEntity);

            return Records.ApiResponse.success(
                    "File uploaded: " + file.getOriginalFilename(),
                    new Records.UploadFileResponse(
                            userAssetEntity.getAssetId(),
                            userAssetEntity.getType(),
                            userAssetEntity.getUrl()
                    )
            );
        } catch (IOException e) {
            throw new InvalidFileOperation(
                    "File upload failed. Make sure its a supported format.",
                    "INVALID_FILE_UPLOAD",
                    e
            );
        }
    }

    public Records.ApiResponse<List<String>> getImageList(String token) {
        try {
            UserEntity user = userService.getUserEntityFromToken(token);


            return Records.ApiResponse.success("Images loaded successfully.");
        } catch (Exception e) {
            throw new InvalidFileOperation(
                    "Could not load image list.",
                    "INVALID_FILE_LIST",
                    e
            );
        }
    }
}
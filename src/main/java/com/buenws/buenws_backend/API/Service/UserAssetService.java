package com.buenws.buenws_backend.API.Service;

import com.buenws.buenws_backend.API.Entity.UserAssetEntity;
import com.buenws.buenws_backend.API.Entity.UserEntity;
import com.buenws.buenws_backend.API.Exception.Custom.InvalidFileOperation;
import com.buenws.buenws_backend.API.Records.Records;
import com.buenws.buenws_backend.API.Repository.RepositoryRetrieval;
import com.buenws.buenws_backend.API.Repository.Repositories.UserAssetsRepository;
import com.buenws.buenws_backend.Util.FileUtil;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Value;
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

    private final RepositoryRetrieval repositoryRetrieval;
    private final UserAssetsRepository userAssetsRepository;

    private final String UPLOAD_DIR = "src/main/resources/static/images/";

    @Value("${app.public-base-url:http://localhost:8080}")
    private String publicBaseUrl;

    public UserAssetService(RepositoryRetrieval repositoryRetrieval, UserAssetsRepository userAssetsRepository) {
        this.repositoryRetrieval = repositoryRetrieval;
        this.userAssetsRepository = userAssetsRepository;
    }

    @Transactional
    public Records.ApiResponse<Records.UploadFileResponse> handleImageUpload(MultipartFile file, String Token) {
        try {
            UserEntity user = repositoryRetrieval.getUserEntityFromToken(Token);
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

            String imageUrl = publicBaseUrl.replaceAll("/$", "") + "/images/" + user.getId() + "/" + FilePrefix + "_image.png";
            UserAssetEntity userAssetEntity = new UserAssetEntity("image", imageUrl, OutputFile.getPath(), user);
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
            UserEntity user = repositoryRetrieval.getUserEntityFromToken(token);


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

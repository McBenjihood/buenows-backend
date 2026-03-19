package com.buenws.buenws_backend;

import com.buenws.buenws_backend.API.Entity.UserEntity;
import com.buenws.buenws_backend.API.Records.UserRecords;
import com.buenws.buenws_backend.API.Repository.UserRepository;
import com.buenws.buenws_backend.API.Service.FileService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;


@SpringBootTest
class BuenowsBackendApplicationTests {

	//Arrange
	@Autowired
	FileService fileService;
	@Autowired
	UserRepository userRepository;

	@Test
	void TestLoadUserByUsername(){
		//Act
		Optional<UserEntity> userEntity = userRepository.findByEmail("benjaminmikagerresheim@gmail.com");

		//Assert
		if (userEntity.isPresent()){
			UserEntity user = userEntity.get();
			assertAll("User Details Verification",
					() -> assertEquals("benjaminmikagerresheim@gmail.com", user.getEmail(), "Username check failed"),
					() -> assertEquals("{bcrypt}$2a$10$4xJ5EPaKDVUjh7UcCwKrc.BI.AOCt4mMI.86CxYw899ZxutM0hf1e", user.getPassword(), "Password check failed"),
					() -> assertTrue(user.getAuthorities().contains("ROLE_USER"), "Role check failed")
			);
		}else {
			fail();
		}
	}

	@Test
	void TestMultipartImageUpload() throws IOException {
		//Arrange
		String UPLOAD_DIR = "uploads/";
		FileInputStream fis = new FileInputStream("src/main/resources/static/images/image.png");

		MockMultipartFile file = new MockMultipartFile(
				"file",
				"image.png",
				"image/png",
				fis
		);

		//Act
		UserRecords.ApiResponse<Void> response = fileService.handleFileUpload(file, UPLOAD_DIR,"");

		//Assert
		assertTrue(response.message().contains("uploaded"));
	}

	@Test
	void TestListDirContent(){
		//Arrange
		String directoryPath = "src/main/resources/static/images/" + "019c768d-78b0-76f3-85b4-83a7cf57e9e5";

		//Act
		File[] files = fileService.listDirContent(directoryPath);

		//Assert
		assertNotNull(files);

	}
}
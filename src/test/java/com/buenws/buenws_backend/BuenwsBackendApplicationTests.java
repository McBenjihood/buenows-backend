package com.buenws.buenws_backend;

import com.buenws.buenws_backend.api.records.UserRecords;
import com.buenws.buenws_backend.api.service.FileService;
import com.buenws.buenws_backend.api.service.userdetails.CustomUserDetailsService;
import com.buenws.buenws_backend.util.BuenowsUtil;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.core.userdetails.UserDetails;

import java.io.FileInputStream;
import java.io.IOException;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;


@SpringBootTest
class BuenwsBackendApplicationTests {

	//Arrange
	@Autowired
	CustomUserDetailsService userDetailsSerivce;

	@Autowired
	FileService fileService;
/*
	@BeforeEach
	void setUp(){
		userDetailsSerivce = mock(CustomUserDetailsService.class);
		tokenService = mock(TokenService.class);
		handlerExceptionResolver = mock(HandlerExceptionResolver.class);
		SecurityContextHolder.clearContext();
	}
 */

	@Test
	void TestLoadUserByUsername(){
		//Act
		UserDetails userDetails = userDetailsSerivce.loadUserByUsername("benjaminmikagerresheim@gmail.com");

		//Assert
		assertAll("User Details Verification",
				() -> assertEquals("benjaminmikagerresheim@gmail.com", userDetails.getUsername(), "Username check failed"),
				() -> assertEquals("{bcrypt}$2a$10$4xJ5EPaKDVUjh7UcCwKrc.BI.AOCt4mMI.86CxYw899ZxutM0hf1e", userDetails.getPassword(), "Password check failed"),
				() -> assertTrue(userDetails.getAuthorities().stream()
						.anyMatch(a -> a.getAuthority().equals("ROLE_USER")), "Role check failed")
		);
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
		UserRecords.ApiResponse<Void> response = fileService.handleFileUpload(file, UPLOAD_DIR);

		//Assert
		assertTrue(response.message().contains("uploaded"));
	}
}
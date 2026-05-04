package com.buenws.buenws_backend;

import com.buenws.buenws_backend.API.Entity.UserEntity;
import com.buenws.buenws_backend.API.Repository.UserRepository;
import com.buenws.buenws_backend.API.Service.UserAssetService;
import com.buenws.buenws_backend.Util.MailUtil;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.SpringVersion;


import java.io.IOException;
import java.util.Optional;


import static org.junit.jupiter.api.Assertions.*;


@SpringBootTest
class BuenowsBackendApplicationTests {

	//Arrange
	@Autowired
	MailUtil mailUtil;

	//Need to redo this test
	@Test
	void TestSendOTPMail() throws IOException {
		//Arrange
		String recipient = "benjaminmikagerresheim@gmail.com";
		String subject = "Test Subject";
		String newOTP = "123456";

		//Act & Assert
		assertTrue(mailUtil.SendOTPMail(recipient, subject, newOTP));
	}

}
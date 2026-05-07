package com.buenws.buenws_backend;

import com.buenws.buenws_backend.API.Entity.UserEntity;
import com.buenws.buenws_backend.API.Repository.Repositories.UserRepository;
import com.buenws.buenws_backend.API.Service.UserAssetService;
import com.buenws.buenws_backend.Util.MailUtil;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Refill;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.SpringVersion;


import java.io.IOException;
import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;


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
		String first_name = "John";

		//Act & Assert
		assertTrue(mailUtil.SendOTPMail(recipient, subject, newOTP, first_name));
	}

	@Test
	void TestBasicBucket4JRateLimitingFunctionality(){
		Bandwidth limit = Bandwidth.classic(1, Refill.intervally(1, Duration.ofSeconds(2)));
		Bucket bucket = Bucket.builder()
				.addLimit(limit)
				.build();
		assertTrue(bucket.tryConsume(1));
		assertFalse(bucket.tryConsume(1));
		Executors.newScheduledThreadPool(1)
				.schedule(() -> assertTrue(bucket.tryConsume(1)), 2, TimeUnit.SECONDS);
	}

}
package com.buenws.buenws_backend;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Refill;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;


import java.time.Duration;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;


import static org.junit.jupiter.api.Assertions.*;


@SpringBootTest
class BuenowsBackendApplicationTests {

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
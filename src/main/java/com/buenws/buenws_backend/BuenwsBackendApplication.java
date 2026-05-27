package com.buenws.buenws_backend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class BuenwsBackendApplication {

	public static void main(String[] args) {
		SpringApplication.run(BuenwsBackendApplication.class, args);
	}

}

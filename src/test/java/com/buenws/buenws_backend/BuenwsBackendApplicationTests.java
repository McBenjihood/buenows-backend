package com.buenws.buenws_backend;

import com.buenws.buenws_backend.api.user.service.CustomUserDetailsService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.core.userdetails.UserDetails;

import static org.junit.jupiter.api.Assertions.*;


@SpringBootTest
class BuenwsBackendApplicationTests {

	//Arrange
	@Autowired
	CustomUserDetailsService userDetailsSerivce;

	@Test
	void TestLoadUserByUsername(){
		//Act
		UserDetails userDetails = userDetailsSerivce.loadUserByUsername("admin_user");

		//Assert
		assertAll("User Details Verification",
				() -> assertEquals("admin_user", userDetails.getUsername(), "Username check failed"),
				() -> assertEquals("admin123", userDetails.getPassword(), "Password check failed"),
				() -> assertTrue(userDetails.getAuthorities().stream()
						.anyMatch(a -> a.getAuthority().equals("USER")), "Role check failed")
		);
	}
}

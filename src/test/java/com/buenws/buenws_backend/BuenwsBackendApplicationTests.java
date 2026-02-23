package com.buenws.buenws_backend;

import com.buenws.buenws_backend.api.configuration.BearerTokenAuthFilter;
import com.buenws.buenws_backend.api.entity.UserEntity;
import com.buenws.buenws_backend.api.service.tokens.TokenService;
import com.buenws.buenws_backend.api.service.userdetails.CustomUserDetailsService;
import com.nimbusds.jose.JOSEException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.servlet.HandlerExceptionResolver;

import java.io.IOException;
import java.text.ParseException;
import java.util.Date;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;


@SpringBootTest
class BuenwsBackendApplicationTests {

	//Arrange
	@Autowired
	CustomUserDetailsService userDetailsSerivce;
	private TokenService tokenService;
	private HandlerExceptionResolver handlerExceptionResolver;

	@BeforeEach
	void setUp(){
		tokenService = mock(TokenService.class);
		handlerExceptionResolver = mock(HandlerExceptionResolver.class);
		SecurityContextHolder.clearContext();
	}

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

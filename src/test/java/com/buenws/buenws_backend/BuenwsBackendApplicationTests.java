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

	private BearerTokenAuthFilter filter;
	private HttpServletRequest request;
	private HttpServletResponse response;
	private FilterChain filterChain;
	private TokenService tokenService;

	@BeforeEach
	void setUp(){
		tokenService = mock(TokenService.class);
		filter = new BearerTokenAuthFilter(tokenService);
		request = mock(HttpServletRequest.class);
		response = mock(HttpServletResponse.class);
		filterChain = mock(FilterChain.class);

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

	@Test
	void TestExpiredJWT() throws ParseException, JOSEException, ServletException, IOException {
		//Arrange
		String mockToken = "eyJhbGciOiJIUzI1NiJ9.eyJpc3MiOiJodHRwczovL2J1ZW5vd3Mub3JnIiwic3ViIjoiYmVuamFtaW5taWthZ2VycmVzaGVpbUBnbWFpbC5jb20iLCJleHAiOjE3Njk2ODM4MTMsImlhdCI6MTc2OTY4MDIxMywicm9sZXMiOlsiUk9MRV9VU0VSIl19.oWhonw_z0p8QDPN40VQugGFWnDrZ_NueO7E1Tv6dtFY";
		Date expDate = new Date(System.currentTimeMillis() - 1000000);

		when(request.getHeader("Authorization")).thenReturn("Bearer " + mockToken);
		when(tokenService.parseTokenFromHeader(anyString())).thenReturn(mockToken);
		when(tokenService.getExpirationFromToken(mockToken)).thenReturn(expDate);
		when(tokenService.validateToken(mockToken)).thenReturn(Optional.of(new UserEntity()));

		//Act
		filter.doFilter(request, response, filterChain);

		//Assert
		assertNull(SecurityContextHolder.getContext().getAuthentication());
	}
}

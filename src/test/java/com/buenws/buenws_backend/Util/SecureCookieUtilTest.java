package com.buenws.buenws_backend.Util;

import com.buenws.buenws_backend.API.Records.Records;
import com.buenws.buenws_backend.API.Service.Tokens.TokenService;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.web.MockHttpServletResponse;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

class SecureCookieUtilTest {

    @Test
    void addAuthCookiesSetsAccessAndRefreshCookiesWithSecureFlags() {
        MockHttpServletResponse response = new MockHttpServletResponse();

        SecureCookieUtil.addAuthCookies(
                response,
                new Records.SuccessfulAuthResponse("jwt-token", "refresh-token")
        );

        List<String> cookies = response.getHeaders(HttpHeaders.SET_COOKIE);

        assertTrue(cookies.stream().anyMatch(cookie -> cookie.startsWith(TokenService.ACCESS_TOKEN_COOKIE + "=")));
        assertTrue(cookies.stream().anyMatch(cookie -> cookie.startsWith(TokenService.REFRESH_TOKEN_COOKIE + "=")));
        assertTrue(cookies.stream().allMatch(cookie -> cookie.contains("HttpOnly")));
        assertTrue(cookies.stream().allMatch(cookie -> cookie.contains("Secure")));
        assertTrue(cookies.stream().allMatch(cookie -> cookie.contains("SameSite=Lax")));
    }

    @Test
    void clearAuthCookiesClearsAccessAndRefreshCookies() {
        MockHttpServletResponse response = new MockHttpServletResponse();

        SecureCookieUtil.clearAuthCookies(response);

        List<String> cookies = response.getHeaders(HttpHeaders.SET_COOKIE);

        assertTrue(cookies.stream().anyMatch(cookie ->
                cookie.startsWith(TokenService.ACCESS_TOKEN_COOKIE + "=") && cookie.contains("Max-Age=0")
        ));
        assertTrue(cookies.stream().anyMatch(cookie ->
                cookie.startsWith(TokenService.REFRESH_TOKEN_COOKIE + "=") && cookie.contains("Max-Age=0")
        ));
    }
}

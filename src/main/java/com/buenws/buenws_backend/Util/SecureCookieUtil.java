package com.buenws.buenws_backend.Util;

import com.buenws.buenws_backend.API.Records.Records;
import com.buenws.buenws_backend.API.Configuration.SecurityCookieProperties;
import com.buenws.buenws_backend.API.Service.Tokens.TokenService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
public class SecureCookieUtil {
    private final SecurityCookieProperties cookieProperties;

    public SecureCookieUtil(SecurityCookieProperties cookieProperties) {
        this.cookieProperties = cookieProperties;
    }

    public void addAuthCookies(HttpServletResponse response, Records.SuccessfulAuthResponse authResponse) {
        ResponseCookie refreshCookie = ResponseCookie.from(TokenService.REFRESH_TOKEN_COOKIE, authResponse.RefreshToken())
                .httpOnly(true)
                .secure(cookieProperties.secure())
                .sameSite("Lax")
                .path("/api/user/auth")
                .maxAge(Duration.ofDays(7))
                .build();

        response.addHeader(HttpHeaders.SET_COOKIE, refreshCookie.toString());
    }

    public void clearAuthCookies(HttpServletResponse response) {
        ResponseCookie refreshCookie = ResponseCookie.from(TokenService.REFRESH_TOKEN_COOKIE, "")
                .httpOnly(true)
                .secure(cookieProperties.secure())
                .sameSite("Lax")
                .path("/api/user/auth")
                .maxAge(0)
                .build();

        response.addHeader(HttpHeaders.SET_COOKIE, refreshCookie.toString());
    }

    public String getTokenFromCookie(HttpServletRequest request, String cookieName) {
        if (request.getCookies() == null) {
            return null;
        }

        for (jakarta.servlet.http.Cookie cookie : request.getCookies()) {
            if (cookieName.equals(cookie.getName())) {
                return cookie.getValue();
            }
        }

        return null;
    }
}

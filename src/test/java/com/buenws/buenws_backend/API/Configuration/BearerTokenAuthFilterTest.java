package com.buenws.buenws_backend.API.Configuration;

import com.buenws.buenws_backend.API.Service.Tokens.TokenService;
import jakarta.servlet.ServletException;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.servlet.HandlerExceptionResolver;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

class BearerTokenAuthFilterTest {
    @Test
    void missingAuthorizationHeaderContinuesWithoutParsingToken() throws ServletException, IOException {
        TokenService tokenService = mock(TokenService.class);
        HandlerExceptionResolver exceptionResolver = mock(HandlerExceptionResolver.class);
        BearerTokenAuthFilter filter = new BearerTokenAuthFilter(tokenService, exceptionResolver);

        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/admin/test");
        request.setServletPath("/api/admin/test");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(request, response, chain);

        assertEquals(200, response.getStatus());
        verify(tokenService, never()).parseTokenFromHeader(null);
    }
}

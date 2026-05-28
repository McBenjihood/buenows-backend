package com.buenws.buenws_backend.API.Chatbot;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ChatbotRequestUtilTest {
    @Test
    void ignoresForwardedHeadersFromUntrustedRemoteAddress() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("203.0.113.10");
        request.addHeader("X-Forwarded-For", "198.51.100.25");
        request.addHeader("X-Real-IP", "198.51.100.26");

        assertEquals("203.0.113.10", ChatbotRequestUtil.getClientKey(request, "loopback"));
    }

    @Test
    void usesFirstForwardedAddressFromTrustedPrivateProxy() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("10.0.2.15");
        request.addHeader("X-Forwarded-For", "198.51.100.25, 10.0.2.15");

        assertEquals("198.51.100.25", ChatbotRequestUtil.getClientKey(request, "private"));
    }

    @Test
    void supportsExplicitCidrTrustedProxy() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("172.31.8.12");
        request.addHeader("X-Forwarded-For", "198.51.100.25");

        assertEquals("198.51.100.25", ChatbotRequestUtil.getClientKey(request, "172.31.0.0/16"));
    }

    @Test
    void fallsBackToRemoteAddressWhenForwardedAddressIsInvalid() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("10.0.2.15");
        request.addHeader("X-Forwarded-For", "not-an-ip");

        assertEquals("10.0.2.15", ChatbotRequestUtil.getClientKey(request, "private"));
    }
}

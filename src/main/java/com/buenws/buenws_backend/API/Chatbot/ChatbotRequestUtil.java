package com.buenws.buenws_backend.API.Chatbot;

import jakarta.servlet.http.HttpServletRequest;

public final class ChatbotRequestUtil {
    private ChatbotRequestUtil() {}

    public static String getClientKey(HttpServletRequest request) {
        if (request == null) return "unknown";
        String forwardedFor = request.getHeader("X-Forwarded-For");
        if (forwardedFor != null && !forwardedFor.isBlank()) {
            String firstIp = forwardedFor.split(",")[0].trim();
            if (!firstIp.isBlank() && !"unknown".equalsIgnoreCase(firstIp)) return firstIp;
        }
        String realIp = request.getHeader("X-Real-IP");
        if (realIp != null && !realIp.isBlank()) return realIp.trim();
        String remoteAddr = request.getRemoteAddr();
        return remoteAddr == null || remoteAddr.isBlank() ? "unknown" : remoteAddr;
    }
}

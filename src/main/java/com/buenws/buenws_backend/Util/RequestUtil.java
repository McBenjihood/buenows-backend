package com.buenws.buenws_backend.Util;

import jakarta.servlet.http.HttpServletRequest;

public class RequestUtil {
    public static String getClientIp(HttpServletRequest request) {
        if (request == null) {
            return "unknown";
        }

        String xff = request.getHeader("X-Forwarded-For");

        if (xff != null && !xff.isBlank()) {
            return xff.split(",")[0].trim();
        }

        return request.getRemoteAddr();
    }
}

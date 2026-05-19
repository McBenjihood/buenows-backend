package com.buenws.buenws_backend.Util;

import jakarta.servlet.http.HttpServletRequest;

public class RequestUtil {
    public static String getClientIp(HttpServletRequest request) {
        if (request == null) {
            System.out.println("Non valid Request");
            return "unknown";
        }
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            System.out.println("Valid X-Forwarded-For header found in Request");
            String[] ips = xForwardedFor.split(",");
            String clientIp = ips[0].trim();
            if (!clientIp.isEmpty() && !"unknown".equalsIgnoreCase(clientIp)) {
                return clientIp;
            }
        }
        return request.getRemoteAddr();
    }
}

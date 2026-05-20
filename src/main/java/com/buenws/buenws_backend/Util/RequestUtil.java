package com.buenws.buenws_backend.Util;

import jakarta.servlet.http.HttpServletRequest;

public class RequestUtil {
    public static String getClientIp(HttpServletRequest request) {
        if (request == null) {
            return "unknown";
        }

        return request.getRemoteAddr();
    }
}

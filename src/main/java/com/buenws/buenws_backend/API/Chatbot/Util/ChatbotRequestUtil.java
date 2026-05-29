package com.buenws.buenws_backend.API.Chatbot.Util;

import com.buenws.buenws_backend.API.Chatbot.Configuration.ChatbotProperties;
import com.buenws.buenws_backend.Util.ClientIpUtil;
import jakarta.servlet.http.HttpServletRequest;

public final class ChatbotRequestUtil {
    private ChatbotRequestUtil() {}

    public static String getClientKey(HttpServletRequest request, ChatbotProperties properties) {
        String trustedProxyCidrs = properties == null ? "" : properties.trustedProxyCidrs();
        return getClientKey(request, trustedProxyCidrs);
    }

    static String getClientKey(HttpServletRequest request, String trustedProxyCidrs) {
        return ClientIpUtil.getClientIp(request, trustedProxyCidrs);
    }
}

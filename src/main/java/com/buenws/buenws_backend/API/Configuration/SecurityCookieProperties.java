package com.buenws.buenws_backend.API.Configuration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class SecurityCookieProperties {
    private final boolean secure;

    public SecurityCookieProperties(@Value("${app.security.cookie-secure:${APP_COOKIE_SECURE:false}}") boolean secure) {
        this.secure = secure;
    }

    public boolean secure() {
        return secure;
    }
}

package com.buenws.buenws_backend.API.Exception.Custom;

import com.buenws.buenws_backend.API.Exception.CustomBaseException;

public class RateLimitException extends CustomBaseException {
    public RateLimitException(String message, String errorCode) {
        super(message, errorCode);
    }

    public RateLimitException(String message, String errorCode, Exception e) {
        super(message, errorCode, e);
    }
}

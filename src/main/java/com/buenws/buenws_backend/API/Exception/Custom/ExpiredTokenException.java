package com.buenws.buenws_backend.API.Exception.Custom;

import com.buenws.buenws_backend.API.Exception.CustomBaseException;

public class ExpiredTokenException extends CustomBaseException {
    public ExpiredTokenException(String message, String errorCode) {
        super(message, errorCode);
    }
    public ExpiredTokenException(String message, String errorCode, Exception e) {
        super(message, errorCode, e);
    }
}

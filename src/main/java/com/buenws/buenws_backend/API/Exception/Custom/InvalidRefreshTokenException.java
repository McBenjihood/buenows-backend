package com.buenws.buenws_backend.API.Exception.Custom;

import com.buenws.buenws_backend.API.Exception.CustomBaseException;

public class InvalidRefreshTokenException extends CustomBaseException {
    public InvalidRefreshTokenException(String message, String errorCode) {
        super(message, errorCode);
    }
    public InvalidRefreshTokenException(String message, String errorCode, Exception e) {
        super(message, errorCode, e);
    }
}

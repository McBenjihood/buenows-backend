package com.buenws.buenws_backend.api.exception.customExceptions;

import com.buenws.buenws_backend.api.exception.CustomBaseException;

public class ExpiredTokenException extends CustomBaseException {
    public ExpiredTokenException(String message, String errorCode) {
        super(message, errorCode);
    }
    public ExpiredTokenException(String message, String errorCode, Exception e) {
        super(message, errorCode, e);
    }
}

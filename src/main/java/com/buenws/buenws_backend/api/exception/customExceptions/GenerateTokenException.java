package com.buenws.buenws_backend.api.exception.customExceptions;

import com.buenws.buenws_backend.api.exception.CustomBaseException;

public class GenerateTokenException extends CustomBaseException {
    public GenerateTokenException(String message, String errorCode) {
        super(message, errorCode);
    }
    public GenerateTokenException(String message, String errorCode, Exception e) {
        super(message, errorCode, e);
    }
}

package com.buenws.buenws_backend.API.Exception.Custom;

import com.buenws.buenws_backend.API.Exception.CustomBaseException;

public class GenerateTokenException extends CustomBaseException {
    public GenerateTokenException(String message, String errorCode) {
        super(message, errorCode);
    }
    public GenerateTokenException(String message, String errorCode, Exception e) {
        super(message, errorCode, e);
    }
}

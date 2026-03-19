package com.buenws.buenws_backend.API.Exception.Custom;

import com.buenws.buenws_backend.API.Exception.CustomBaseException;

public class ParseTokenException extends CustomBaseException {
    public ParseTokenException(String message, String errorCode) {
        super(message, errorCode);
    }
    public ParseTokenException(String message, String errorCode, Exception e) {
        super(message, errorCode, e);
    }
}

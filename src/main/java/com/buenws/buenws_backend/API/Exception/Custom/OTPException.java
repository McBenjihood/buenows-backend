package com.buenws.buenws_backend.API.Exception.Custom;

import com.buenws.buenws_backend.API.Exception.CustomBaseException;

public class OTPException extends CustomBaseException {
    public OTPException(String message, String errorCode) {
        super(message, errorCode);
    }
    public OTPException(String message, String errorCode, Exception e) {
        super(message, errorCode, e);
    }
}

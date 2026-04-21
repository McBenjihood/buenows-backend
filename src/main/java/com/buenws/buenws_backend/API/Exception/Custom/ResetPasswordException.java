package com.buenws.buenws_backend.API.Exception.Custom;

import com.buenws.buenws_backend.API.Exception.CustomBaseException;

public class ResetPasswordException extends CustomBaseException {
    public ResetPasswordException(String message, String errorCode) {
        super(message, errorCode);
    }
    public ResetPasswordException(String message, String errorCode, Exception e) {
        super(message, errorCode, e);
    }
}

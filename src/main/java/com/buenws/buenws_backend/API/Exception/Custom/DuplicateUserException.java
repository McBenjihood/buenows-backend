package com.buenws.buenws_backend.API.Exception.Custom;

import com.buenws.buenws_backend.API.Exception.CustomBaseException;

public class DuplicateUserException extends CustomBaseException {
    public DuplicateUserException(String message, String errorCode) {
        super(message, errorCode);
    }
    public DuplicateUserException(String message, String errorCode, Exception e) {
        super(message, errorCode, e);
    }
}

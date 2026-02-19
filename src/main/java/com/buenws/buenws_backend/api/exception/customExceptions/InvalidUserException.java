package com.buenws.buenws_backend.api.exception.customExceptions;

import com.buenws.buenws_backend.api.exception.CustomBaseException;

public class InvalidUserException extends CustomBaseException {
    public InvalidUserException(String message, String errorCode) {
        super(message, errorCode);
    }
    public InvalidUserException(String message, String errorCode, Exception e) {
        super(message, errorCode, e);
    }
}

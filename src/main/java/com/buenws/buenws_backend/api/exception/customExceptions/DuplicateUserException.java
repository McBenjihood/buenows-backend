package com.buenws.buenws_backend.api.exception.customExceptions;

import com.buenws.buenws_backend.api.exception.CustomBaseException;

public class DuplicateUserException extends CustomBaseException {
    public DuplicateUserException(String message, String errorCode) {
        super(message, errorCode);
    }
    public DuplicateUserException(String message, String errorCode, Exception e) {
        super(message, errorCode, e);
    }
}

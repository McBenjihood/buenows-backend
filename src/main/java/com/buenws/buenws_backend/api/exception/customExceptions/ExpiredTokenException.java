package com.buenws.buenws_backend.api.exception.customExceptions;

public class ExpiredTokenException extends RuntimeException {
    public ExpiredTokenException(String message) {
        super(message);
    }
}

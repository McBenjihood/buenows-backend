package com.buenws.buenws_backend.api.exception.customExceptions;

public class CouldNotCreateResourceException extends RuntimeException {
    public CouldNotCreateResourceException(String message) {
        super(message);
    }
}

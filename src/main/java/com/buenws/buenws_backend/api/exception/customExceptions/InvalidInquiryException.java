package com.buenws.buenws_backend.api.exception.customExceptions;

public class InvalidInquiryException extends RuntimeException {
    public InvalidInquiryException(String message) {
        super(message);
    }
}

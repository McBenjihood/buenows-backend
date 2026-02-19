package com.buenws.buenws_backend.api.exception.customExceptions;

import com.buenws.buenws_backend.api.exception.CustomBaseException;

public class InvalidInquiryException extends CustomBaseException {
    public InvalidInquiryException(String message, String errorCode) {
        super(message, errorCode);
    }
    public InvalidInquiryException(String message, String errorCode, Exception e) {
        super(message, errorCode, e);
    }
}
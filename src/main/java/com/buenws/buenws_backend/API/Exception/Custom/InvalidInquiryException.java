package com.buenws.buenws_backend.API.Exception.Custom;

import com.buenws.buenws_backend.API.Exception.CustomBaseException;

public class InvalidInquiryException extends CustomBaseException {
    public InvalidInquiryException(String message, String errorCode) {
        super(message, errorCode);
    }
    public InvalidInquiryException(String message, String errorCode, Exception e) {
        super(message, errorCode, e);
    }
}
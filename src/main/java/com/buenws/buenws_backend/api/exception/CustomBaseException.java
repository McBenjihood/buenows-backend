package com.buenws.buenws_backend.api.exception;

public class CustomBaseException extends RuntimeException {

    private String errorCode;
    public String getErrorCode() {
        return errorCode;
    }

    //Constructors
    public CustomBaseException(String message, String errorCode) {
        super(message);
        this.errorCode = errorCode;
    }
    public CustomBaseException(String message, String errorCode, Exception e) {
        super(message, e);
        this.errorCode = errorCode;
    }
}

package com.buenws.buenws_backend.API.Exception.Custom;

import com.buenws.buenws_backend.API.Exception.CustomBaseException;

public class InvalidFileOperation extends CustomBaseException {
    public InvalidFileOperation(String message, String errorCode) {
        super(message, errorCode);
    }
    public InvalidFileOperation(String message, String errorCode, Exception e) {
        super(message, errorCode, e);
    }
}

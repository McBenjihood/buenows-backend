package com.buenws.buenws_backend.API.Exception.Custom;

import com.buenws.buenws_backend.API.Exception.CustomBaseException;

public class InvalidInventoryOperationException extends CustomBaseException {

    public InvalidInventoryOperationException(String message, String errorCode) {
        super(message, errorCode);
    }
    public InvalidInventoryOperationException(String message, String errorCode, Exception e) {
        super(message, errorCode, e);
    }
}

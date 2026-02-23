package com.buenws.buenws_backend.api.exception;


import com.buenws.buenws_backend.api.exception.customExceptions.*;
import com.buenws.buenws_backend.api.records.UserRecords;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

@ControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler({
            InvalidInquiryException.class,
            ParseTokenException.class,
            InvalidRefreshTokenException.class,
            ExpiredTokenException.class,
            DuplicateUserException.class
    })
    public ResponseEntity<UserRecords.ApiResponse> handleException(CustomBaseException ex) {
        return ResponseEntity
                .badRequest()
                .body(
                        UserRecords.ApiResponse.error(
                                ex.getMessage(),
                                new UserRecords.ErrorResponseRecord(
                                    ex.getErrorCode()
                                )
                        )
                );
    }

}


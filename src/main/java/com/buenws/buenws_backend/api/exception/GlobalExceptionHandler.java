package com.buenws.buenws_backend.api.exception;


import com.buenws.buenws_backend.api.exception.customExceptions.*;
import com.buenws.buenws_backend.api.records.UserRecords;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

@ControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(InvalidInquiryException.class)
    public ResponseEntity<UserRecords.ApiResponse> handleException(InvalidInquiryException ex) {
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



    @ExceptionHandler(ParseTokenException.class)
    public ResponseEntity<UserRecords.ApiResponse> handleTokenException(ParseTokenException ex) {
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


    @ExceptionHandler(InvalidRefreshTokenException.class)
    public ResponseEntity<UserRecords.ApiResponse> handleInvalidRefreshTokenException(InvalidRefreshTokenException ex){
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

    @ExceptionHandler(ExpiredTokenException.class)
    public ResponseEntity<UserRecords.ApiResponse> handleExpiredTokenException (ExpiredTokenException ex){
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


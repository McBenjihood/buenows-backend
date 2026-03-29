package com.buenws.buenws_backend.API.Exception;


import com.buenws.buenws_backend.API.Exception.Custom.*;
import com.buenws.buenws_backend.API.Records.UserRecords;
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
            DuplicateUserException.class,
            InvalidUserException.class,
            GenerateTokenException.class
    })
    public ResponseEntity<UserRecords.ApiResponse<UserRecords.ErrorResponseRecord>> handleException(CustomBaseException ex) {
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


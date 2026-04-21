package com.buenws.buenws_backend.API.Exception;


import com.buenws.buenws_backend.API.Exception.Custom.*;
import com.buenws.buenws_backend.API.Records.Records;
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
            GenerateTokenException.class,
            InvalidFileOperation.class,
            OTPException.class,
            ResetPasswordException.class
    })
    public ResponseEntity<Records.ApiResponse<Records.ErrorResponse>> handleException(CustomBaseException ex) {
        return ResponseEntity
                .badRequest()
                .body(
                        Records.ApiResponse.error(
                                ex.getMessage(),
                                new Records.ErrorResponse(
                                        ex.getErrorCode()
                                )
                        )
                );
    }
}


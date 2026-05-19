package com.buenws.buenws_backend.API.Exception;

import com.buenws.buenws_backend.API.Exception.Custom.*;
import com.buenws.buenws_backend.API.Records.Records;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.validation.FieldError;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

@ControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Records.ApiResponse<Map<String, String>>> handleValidationExceptions(MethodArgumentNotValidException ex) {
        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach((error) -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            errors.put(fieldName, errorMessage);
        });
        
        String message = "Validation failed: " + errors.values().stream().collect(Collectors.joining(", "));
        
        return ResponseEntity
                .badRequest()
                .body(Records.ApiResponse.error(message, errors));
    }

    @ExceptionHandler({
            ParseTokenException.class,
            InvalidRefreshTokenException.class,
            ExpiredTokenException.class
    })
    public ResponseEntity<Records.ApiResponse<Records.ErrorResponse>> handleAuthException(CustomBaseException ex) {
        return ResponseEntity
                .status(HttpStatus.UNAUTHORIZED)
                .body(
                        Records.ApiResponse.error(
                                ex.getMessage(),
                                new Records.ErrorResponse(ex.getErrorCode())
                        )
                );
    }


    @ExceptionHandler({
            InvalidInquiryException.class,
            DuplicateUserException.class,
            InvalidUserException.class,
            GenerateTokenException.class,
            InvalidFileOperation.class,
            OTPException.class,
            ResetPasswordException.class,
            MailException.class
    })
    public ResponseEntity<Records.ApiResponse<Records.ErrorResponse>> handleBadRequestException(CustomBaseException ex) {
        return ResponseEntity
                .badRequest()
                .body(
                        Records.ApiResponse.error(
                                ex.getMessage(),
                                new Records.ErrorResponse(ex.getErrorCode())
                        )
                );
    }

    @ExceptionHandler({
            RateLimitException.class
    })
    public ResponseEntity<Records.ApiResponse<Records.ErrorResponse>> handleRateLimitException(CustomBaseException ex) {
        return ResponseEntity
                .status(HttpStatus.TOO_MANY_REQUESTS)
                .body(
                        Records.ApiResponse.error(
                                ex.getMessage(),
                                new Records.ErrorResponse(ex.getErrorCode())
                        )
                );
    }
}
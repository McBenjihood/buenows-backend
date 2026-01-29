package com.buenws.buenws_backend.api.exception;


import com.buenws.buenws_backend.api.exception.customExceptions.CouldNotCreateResourceException;
import com.buenws.buenws_backend.api.exception.customExceptions.InvalidInquiryException;
import com.buenws.buenws_backend.api.exception.customExceptions.ParseTokenException;
import com.buenws.buenws_backend.api.exception.customExceptions.UserNotFoundException;
import com.buenws.buenws_backend.api.records.UserRecords;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

@ControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(InvalidInquiryException.class)
    public ResponseEntity<UserRecords.DefaultResponseRecord> handleException(Exception ex) {
        return ResponseEntity
                .badRequest()
                .body(
                        new UserRecords.DefaultResponseRecord(false, ex.getMessage())
                );
    }

    @ExceptionHandler(CouldNotCreateResourceException.class)
    public ResponseEntity<UserRecords.DefaultResponseRecord> handleCouldNotCreateResourceException(Exception ex) {
        return ResponseEntity
                .badRequest()
                .body(
                        new UserRecords.DefaultResponseRecord(false, ex.getMessage())
                );
    }

    @ExceptionHandler(ParseTokenException.class)
    public ResponseEntity<UserRecords.LoginResponseRecord> handleTokenException(Exception ex) {
        return ResponseEntity
                .badRequest()
                .body(
                        new UserRecords.LoginResponseRecord(
                                false,
                                ex.getMessage(),
                                "",
                                "",
                                "",
                                0L,
                                ""
                        )
                );
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<UserRecords.LoginResponseRecord> handleAuthenticationException(Exception ex){
        return ResponseEntity
                .badRequest()
                .body(
                        new UserRecords.LoginResponseRecord(
                                false,
                                "Invalid email or password",
                                "",
                                "",
                                "",
                                0L,
                                ""
                        )
                );
    }

    @ExceptionHandler(UserNotFoundException.class)
    public ResponseEntity<UserRecords.DefaultResponseRecord> handleUserNotFoundException(Exception ex) {
        return ResponseEntity
                .badRequest()
                .body(
                        new UserRecords.DefaultResponseRecord(false, ex.getMessage())
                );
    }

}


package com.buenws.buenws_backend.api.exception;


import com.buenws.buenws_backend.api.exception.customExceptions.CouldNotCreateResourceException;
import com.buenws.buenws_backend.api.exception.customExceptions.InvalidInquiryException;
import com.buenws.buenws_backend.api.records.UserRecords;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

@ControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(InvalidInquiryException.class)
    public ResponseEntity<UserRecords.FormSubmissionResponseRecord> handleException(Exception ex) {
        return ResponseEntity
                .badRequest()
                .body(
                        new UserRecords.FormSubmissionResponseRecord(false, ex.getMessage())
                );
    }

    @ExceptionHandler(CouldNotCreateResourceException.class)
    public ResponseEntity<UserRecords.FormSubmissionResponseRecord> handleCouldNotCreateResourceException(Exception ex) {
        return ResponseEntity
                .badRequest()
                .body(
                        new UserRecords.FormSubmissionResponseRecord(false, ex.getMessage())
                );
    }
}


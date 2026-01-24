package com.buenws.buenws_backend.api;

public class records
{
    public record FormSubmissionRequestRecord(
            String email,
            String title,
            String message
    ){}

    public record FormSubmissionResponseRecord(
            boolean successful,
            String message
    ){}
}

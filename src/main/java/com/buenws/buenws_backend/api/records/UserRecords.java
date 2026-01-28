package com.buenws.buenws_backend.api.records;

public class UserRecords
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

    public record CredentialsSubmitRequestRecord(
            String email,
            String password
    ){}

    public record CredentialsResponseRecord(
            boolean successful,
            String message
    ){}
}

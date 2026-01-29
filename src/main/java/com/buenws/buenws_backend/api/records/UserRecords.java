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

    public record RegisterResponseRecord(
            boolean successful,
            String message
    ){}

    public record LoginResponseRecord(
            boolean successful,
            String message,
            String tokenType,
            String token,
            Long expirationDate,
            String email
    ){}

    public record blockedEndpointResponse(
            boolean bool
    ){}

    public record ExpiredTokenResponseRecord(
            String errorCode,
            String message
    ){}
}

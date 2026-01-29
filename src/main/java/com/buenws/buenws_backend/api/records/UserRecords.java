package com.buenws.buenws_backend.api.records;

public class UserRecords
{
    //Responses
    //Default Response
    public record DefaultResponseRecord(
            boolean successful,
            String message
    ){}
    //Specific Responses
    public record RegisterResponseRecord(
            boolean successful,
            String message
    ){}
    public record LoginResponseRecord(
            boolean successful,
            String message,
            String tokenType,
            String token,
            String refresh_token,
            Long expirationDate,
            String email
    ){}
    public record ExpiredTokenResponseRecord(
            String errorCode,
            String message
    ){}
    public record RefreshTokenResponseRecord(
      boolean successful,
      String token,
      String refreshToken
    ){}


    //Requests
    public record FormSubmissionRequestRecord(
            String email,
            String title,
            String message
    ){}
    public record CredentialsSubmitRequestRecord(
            String email,
            String password
    ){}
    public record RefreshTokenRequestRecord(
            String refresh_token
    ){}
}

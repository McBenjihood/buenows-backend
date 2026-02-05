package com.buenws.buenws_backend.api.records;

public class UserRecords
{
    //
    // IMPORTANT TODO: Implement Generic Wrappers so dry principle is still upheld.
    //


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
            String refreshToken,
            String email
    ){}
    public record ExpiredTokenResponseRecord(
            String errorCode,
            String message
    ){}
    public record RefreshTokenResponseRecord(
      boolean successful,
      String message,
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

package com.buenws.buenws_backend.api.records;

public class UserRecords
{
    //Generic Response
    public record ApiResponse<T>(
            boolean successful,
            String message,
            T data
    ){
        //Successful Response
        public static <T> ApiResponse<T> success (String message, T data){
            return new ApiResponse<>(true, message, data);
        }
        public static <T> ApiResponse<T> success (String message){
            return new ApiResponse<>(true, message, null);
        }

        //Unsuccessful Response
        public static <T> ApiResponse<T> error (String message, T data){
            return new ApiResponse<>(false, message, data);
        }
    }

    //Specific Responses
    public record LoginResponseRecord(
            String JWTToken,
            String refreshToken,
            String email
    ){}
    public record RefreshTokenResponseRecord(
            String JWTToken,
            String refreshToken
    ){}
    public record ErrorResponseRecord(
            String errorCode
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

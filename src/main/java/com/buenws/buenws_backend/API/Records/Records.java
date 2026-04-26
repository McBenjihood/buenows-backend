package com.buenws.buenws_backend.API.Records;

import java.lang.reflect.Array;

public class Records
{
    //Generic Response
    public record ApiResponse<T>(
            boolean successful,
            String message,
            T data
    ){
        public static <T> ApiResponse<T> success (String message, T data){
            return new ApiResponse<>(true, message, data);
        }
        public static <T> ApiResponse<T> success (String message){
            return new ApiResponse<>(true, message, null);
        }

        public static <T> ApiResponse<T> error (String message, T data){
            return new ApiResponse<>(false, message, data);
        }
    }

    //Specific Responses
    public record SuccessfulAuthResponse(
            String JWT,
            String RefreshToken
    ){}
    public record ErrorResponse(
            String errorCode
    ){}
    public record UploadFileResponse(
            long asset_id,
            String type,
            String url
    ){}
    public record VerifyOTPResponse(
            String verified_token
    ){}
    public record InquiryResponse(
            long inquiry_id,
            String email,
            String title,
            String message,
            String created_at
    ){}
    public record AdminUserResponse(
            String user_id,
            String email,
            String first_name,
            String last_name,
            java.util.List<String> authorities,
            String created_at
    ){}
    public record AdminUpdateRoleRequest(
            String role
    ){}
    public record AdminUpdateUserProfileRequest(
            String first_name,
            String last_name
    ){}

    //Requests
    public record FormSubmissionRequest(
            String email,
            String title,
            String message
    ){}
    public record CredentialsSubmitRequest(
            String email,
            String first_name,
            String last_name,
            String password
    ){}
    public record RefreshTokenRequest(
            String refresh_token
    ){}
    public record InitResetPasswordRequest(
            String email
    ){}
    public record VerifyOTPRequest(
            String email,
            String otp
    ){}
    public record ChangePasswordRequest(
            String password,
            String verified_token
    ){}

    //Image Stuff (Not ready yet.)
    public record Image(

    ){}
    public record getImageListRequest(
            Array[] Images
    ){}
}
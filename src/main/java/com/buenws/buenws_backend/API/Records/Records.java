package com.buenws.buenws_backend.API.Records;

import jakarta.validation.constraints.*;
import java.lang.reflect.Array;
import java.util.UUID;

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
        public static <T> ApiResponse<T> error (String message){
            return new ApiResponse<>(false, message, null);
        }
    }

    //Specific Responses
    public record SuccessfulAuthResponse(
            String JWT,
            String RefreshToken
    ){}
    public record AuthCheckResponse(
            String email,
            java.util.List<String> authorities
    ){}
    public record CsrfTokenResponse(
            String header_name,
            String parameter_name,
            String token
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
            UUID verified_token
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
            @NotBlank(message = "Role is required")
            String role,
            @NotNull(message = "UserID is required")
            UUID userID
    ){}
    public record AdminUpdateUserProfileRequest(
            @NotBlank(message = "First name is required")
            @Size(max = 50)
            String first_name,
            @NotBlank(message = "Last name is required")
            @Size(max = 50)
            String last_name,
            @NotNull(message = "UserID is required")
            UUID userID
    ){}

    //Requests
    public record FormSubmissionRequest(
            @NotBlank(message = "Email is required")
            @Email(message = "Invalid contact format")
            String email,
            @NotBlank(message = "Title is required")
            @Size(min = 3, max = 100, message = "Title must be between 3 and 100 characters")
            String title,
            @NotBlank(message = "Message is required")
            @Size(min = 10, max = 2000, message = "Message must be between 10 and 2000 characters")
            String message
    ){}

    public record RegisterCredentialsSubmitRequest(
            @NotBlank(message = "First name is required")
            @Size(max = 50)
            String first_name,
            @NotBlank(message = "Last name is required")
            @Size(max = 50)
            String last_name,
            @NotBlank(message = "Password is required")
            @Size(min = 8, max = 128, message = "Password must be between 8 and 128 characters long")
            String password,
            @NotNull(message = "UUID must not be null")
            UUID verified_token
    ){}
    public record CredentialsSubmitRequest(
            @NotBlank(message = "Email is required")
            @Email(message = "Invalid contact format")
            String email,
            @NotBlank(message = "Password is required")
            @Size(min = 8, max = 128, message = "Password must be between 8 and 128 characters long")
            String password
    ){}
    public record RefreshTokenRequest(
            @NotBlank(message = "Refresh token is required")
            String refresh_token
    ){}
    public record RequestOTPRequest(
            @Email(message = "Invalid contact format")
            @NotBlank(message = "Email is required")
            String contact_information,
            String language
    ){}
    public record VerifyOTPRequest(
            @NotBlank(message = "Email is required")
            @Email(message = "Invalid contact format")
            String contact_information,
            @NotBlank(message = "OTP is required")
            @Size(min = 6, max = 6, message = "OTP must be 6 characters")
            String otp
    ){}
    public record ChangePasswordRequest(
            @NotBlank(message = "Password is required")
            @Size(min = 8, max = 128, message = "Password must be between 8 and 128 characters long")
            String password,
            @NotNull(message = "UUID must not be null")
            UUID verified_token
    ){}

    public record DeleteInquiryRequest(
            @NotNull(message = "InquiryID is required")
            Long inquiryID
    ){}

    public record DeleteUserRequest(
            @NotNull(message = "userID is required")
            UUID userID
    ){}
}

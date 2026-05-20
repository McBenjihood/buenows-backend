package com.buenws.buenws_backend.API.Service;

import com.buenws.buenws_backend.API.Entity.RefreshTokenEntity;
import com.buenws.buenws_backend.API.Entity.OTPAuthEntity;
import com.buenws.buenws_backend.API.Entity.UserEntity;
import com.buenws.buenws_backend.API.Exception.Custom.*;
import com.buenws.buenws_backend.API.Records.Records;
import com.buenws.buenws_backend.API.Repository.Repositories.RefreshTokenRepository;
import com.buenws.buenws_backend.API.Repository.RepositoryRetrieval;
import com.buenws.buenws_backend.API.Repository.Repositories.OTPAuthRepository;
import com.buenws.buenws_backend.API.Repository.Repositories.UserRepository;
import com.buenws.buenws_backend.API.Service.Tokens.TokenService;
import com.buenws.buenws_backend.Util.CryptographyUtil;
import com.buenws.buenws_backend.Util.TimeUtil;
import com.nimbusds.jose.JOSEException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Service
public class UserService {

    //Constructor Injection
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final TokenService tokenService;
    private final AuthenticationManager authenticationManager;
    private final RefreshTokenRepository refreshTokenRepository;
    private final OTPAuthRepository OTPAuthRepository;
    private final RepositoryRetrieval repositoryRetrieval;
    private final OTPAuthService otpAuthService;

    @Value("${HASH_SALT}")
    private String salt;

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder, TokenService tokenService,
                       AuthenticationManager authenticationManager, RefreshTokenRepository refreshTokenRepository,
                       OTPAuthRepository OTPAuthRepository, RepositoryRetrieval repositoryRetrieval, OTPAuthService otpAuthService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.tokenService = tokenService;
        this.authenticationManager = authenticationManager;
        this.refreshTokenRepository = refreshTokenRepository;
        this.OTPAuthRepository = OTPAuthRepository;
        this.repositoryRetrieval = repositoryRetrieval;
        this.otpAuthService = otpAuthService;
    }

    @Transactional
    public Records.ApiResponse<Records.SuccessfulAuthResponse> RegisterUserWithCredentials(
            Records.RegisterCredentialsSubmitRequest credentialsSubmitRequest) {

        OTPAuthEntity otpAuthEntity = repositoryRetrieval.getResetCodeEntityFromVerified_Token(credentialsSubmitRequest.verified_token());
        if (otpAuthService.checkOTPAuthEntity(otpAuthEntity)){
            String normalizedEmail = otpAuthEntity.getContact().trim().toLowerCase();

            UserEntity user = new UserEntity();

            user.setFirst_name(normalizeRequiredText(credentialsSubmitRequest.first_name(), "First name"));
            user.setLast_name(normalizeRequiredText(credentialsSubmitRequest.last_name(), "Last name"));

            user.setAuthorities(new ArrayList<>(List.of("ROLE_USER")));
            user.setEmail(normalizedEmail);
            user.setPassword(passwordEncoder.encode(credentialsSubmitRequest.password()));

            String refreshToken = tokenService.generateRefreshToken();

            user.setRefreshTokenEntity(
                    new RefreshTokenEntity(
                            CryptographyUtil.HashString(refreshToken, salt),
                            TimeUtil.getCurrentTime(),
                            TimeUtil.getWeekFromNow(),
                            user
                    )
            );

            try {

                userRepository.save(user);
            } catch (DataIntegrityViolationException e) {
                throw new DuplicateUserException(
                        "User with Email: '" + normalizedEmail + "' already exists",
                        "DUPLICATE_USER",
                        e
                );
            }

            otpAuthService.disableFactor(otpAuthEntity);


            String JWT;
            try {
                JWT = tokenService.generateJWTToken(user);
            } catch (JOSEException e) {
                throw new GenerateTokenException("Error login User in. Please try again.", "GENERATE_TOKEN_ERROR");
            }

            return Records.ApiResponse.success(
                    "User with Email: '" + normalizedEmail + "' registered successfully",
                    new Records.SuccessfulAuthResponse(
                            JWT,
                            refreshToken
                    )
            );
        }else {
            throw new OTPException("Your Email-Verification couldn't be validated. Please try again.", "INVALID_OTP");
        }

    }

    // Login Logic
    @Transactional
    public Records.ApiResponse<Records.SuccessfulAuthResponse> LoginUserWithCredentials(
            Records.CredentialsSubmitRequest credentialsSubmitRequest) {

        String normalizedEmail = credentialsSubmitRequest.email().trim().toLowerCase();

        try {
            Authentication authenticationRequest = UsernamePasswordAuthenticationToken.unauthenticated(
                    normalizedEmail,
                    credentialsSubmitRequest.password());

            authenticationManager.authenticate(authenticationRequest);

            UserEntity user = repositoryRetrieval.getUserEntityFromEmail(normalizedEmail);

            String JWTToken;
            try {
                JWTToken = tokenService.generateJWTToken(user);
            } catch (JOSEException e) {
                throw new GenerateTokenException("Error login User in. Please try again.", "GENERATE_TOKEN_ERROR");
            }

            String refreshToken = tokenService.generateRefreshToken();
            if (user.getRefreshTokenEntity() == null) {
                user.setRefreshTokenEntity(
                        new RefreshTokenEntity(
                                refreshToken,
                                TimeUtil.getCurrentTime(),
                                TimeUtil.getWeekFromNow(),
                                user
                        )
                );
            }
            user.getRefreshTokenEntity().setToken(CryptographyUtil.HashString(refreshToken, salt) );
            user.getRefreshTokenEntity().setEdited_at(TimeUtil.getCurrentTime());
            user.getRefreshTokenEntity().setExpires_at(TimeUtil.getWeekFromNow());
            userRepository.save(user);

            return Records.ApiResponse.success(
                    "Log in was successful.",
                    new Records.SuccessfulAuthResponse(
                            JWTToken,
                            refreshToken));

        } catch (AuthenticationException e) {
            throw new InvalidUserException("Invalid E-Mail or password.", "INVALID_CREDENTIALS");
        }
    }

    // RefreshToken Logic
    @Transactional
    public Records.ApiResponse<Records.SuccessfulAuthResponse> RefreshToken(String refreshToken) {

        RefreshTokenEntity refreshTokenEntity;
        String JWTToken;
        UserEntity userEntity;

        try {
            refreshTokenEntity = tokenService.validateRefreshToken(refreshToken);
            userEntity = refreshTokenEntity.getUserEntity();
            JWTToken = tokenService.generateJWTToken(userEntity);
        } catch (ParseException | JOSEException exception) {
            throw new ParseTokenException("Please Log in again.", "INVALID_TOKEN");
        }

        String RefreshToken = tokenService.generateRefreshToken();

        refreshTokenEntity.setToken(CryptographyUtil.HashString(RefreshToken, salt));
        refreshTokenEntity.setEdited_at(TimeUtil.getCurrentTime());
        refreshTokenEntity.setExpires_at(TimeUtil.getWeekFromNow());

        refreshTokenRepository.save(refreshTokenEntity);

        return Records.ApiResponse.success(
                "Tokens generated successfully.",
                new Records.SuccessfulAuthResponse(
                        JWTToken,
                        RefreshToken));
    }

    @Transactional
    public Records.ApiResponse<Void> Logout(String refreshToken) {
        if (refreshToken != null && !refreshToken.isBlank()) {
            RefreshTokenEntity refreshTokenEntity;
            try {
                refreshTokenEntity = tokenService.validateRefreshToken(refreshToken);
            } catch (ParseException | JOSEException | RuntimeException e) {
                return Records.ApiResponse.success("Logged out successfully.");
            }

            refreshTokenEntity.setToken(tokenService.generateRefreshToken());
            refreshTokenEntity.setEdited_at(TimeUtil.getCurrentTime());
            refreshTokenEntity.setExpires_at(TimeUtil.getCurrentTime());
            refreshTokenRepository.save(refreshTokenEntity);
        }

        return Records.ApiResponse.success("Logged out successfully.");
    }

    // Reset Password Logic
    @Transactional
    public Records.ApiResponse<Void> RequestOTP(Records.RequestOTPRequest requestOTPRequest, Locale locale) {
        otpAuthService.requestFactor(requestOTPRequest.contact_information(), locale);
        return Records.ApiResponse.success(
                "If an account exists, a verification email has been sent."
        );
    }

    @Transactional
    public Records.ApiResponse<Records.VerifyOTPResponse> VerifyOTP(Records.VerifyOTPRequest verifyOTPRequest) {
        UUID verified_token = otpAuthService.verifyFactor(verifyOTPRequest.contact_information(), verifyOTPRequest.otp());
        return Records.ApiResponse.success(
        "OTP verified.",
            new Records.VerifyOTPResponse(
                verified_token
            )
        );
    }

    @Transactional
    public Records.ApiResponse<Void> ChangePassword(Records.ChangePasswordRequest changePasswordRequest) {
        OTPAuthEntity otpAuthEntity = repositoryRetrieval.getResetCodeEntityFromVerified_Token(changePasswordRequest.verified_token());

        if (otpAuthService.checkOTPAuthEntity(otpAuthEntity)) {
            UserEntity user;
            try {
                user = repositoryRetrieval.getUserEntityFromEmail(otpAuthEntity.getContact());
            } catch (InvalidUserException e) {
                otpAuthService.disableFactor(otpAuthEntity);
                throw new ResetPasswordException("Password couldn't be changed", "INVALID_OTP");
            }

            user.setPassword(passwordEncoder.encode(changePasswordRequest.password()));

            otpAuthService.disableFactor(otpAuthEntity);

            OTPAuthRepository.save(otpAuthEntity);
            userRepository.save(user);

            return Records.ApiResponse.success("Password was successfully changed.");
        }
        throw new ResetPasswordException("Password couldn't be changed", "INVALID_OTP");
    }

    @Transactional(readOnly = true)
    public Records.ApiResponse<List<Records.AdminUserResponse>> getAllUsersForAdmin() {
        List<Records.AdminUserResponse> users = userRepository.findAll().stream()
                .sorted(Comparator.comparing(UserEntity::getCreated_at).reversed())
                .map(this::mapAdminUser)
                .toList();

        return Records.ApiResponse.success("Users loaded successfully.", users);
    }

    @Transactional
    public Records.ApiResponse<Records.AdminUserResponse> updateUserRole(UUID userId, Records.AdminUpdateRoleRequest request) {
        UserEntity targetUser = userRepository.findById(userId)
                .orElseThrow(() -> new InvalidUserException("User not found.", "INVALID_USER"));

        String normalizedRole = normalizeRole(request.role());

        if (!normalizedRole.equals("ROLE_USER") && !normalizedRole.equals("ROLE_ADMIN")) {
            throw new InvalidUserException("Role is not supported.", "INVALID_ROLE");
        }

        if (targetUser.getAuthorities() != null
                && targetUser.getAuthorities().contains("ROLE_ADMIN")
                && normalizedRole.equals("ROLE_USER")
                && isLastAdmin(targetUser)) {
            throw new InvalidUserException("The last admin cannot be downgraded.", "LAST_ADMIN_PROTECTED");
        }

        targetUser.setAuthorities(new ArrayList<>(List.of(normalizedRole)));
        userRepository.save(targetUser);

        return Records.ApiResponse.success(
                "User role updated successfully.",
                mapAdminUser(targetUser)
        );
    }

    @Transactional
    public Records.ApiResponse<Records.AdminUserResponse> updateUserProfile(UUID userId, Records.AdminUpdateUserProfileRequest request) {
        UserEntity targetUser = userRepository.findById(userId)
                .orElseThrow(() -> new InvalidUserException("User not found.", "INVALID_USER"));

        targetUser.setFirst_name(normalizeOptionalText(request.first_name()));
        targetUser.setLast_name(normalizeOptionalText(request.last_name()));

        userRepository.save(targetUser);

        return Records.ApiResponse.success(
                "User profile updated successfully.",
                mapAdminUser(targetUser)
        );
    }

    @Transactional
    public Records.ApiResponse<Void> deleteUserForAdmin(UUID userId) {
        UserEntity targetUser = userRepository.findById(userId)
                .orElseThrow(() -> new InvalidUserException("User not found.", "INVALID_USER"));

        Authentication currentAuthentication = SecurityContextHolder.getContext().getAuthentication();

        if (currentAuthentication == null || currentAuthentication.getName() == null) {
            throw new InvalidUserException("Current admin could not be identified.", "INVALID_AUTHENTICATION");
        }

        String currentAdminEmail = currentAuthentication.getName();

        if (targetUser.getEmail() != null && targetUser.getEmail().equalsIgnoreCase(currentAdminEmail)) {
            throw new InvalidUserException("You cannot delete your own account.", "SELF_DELETE_NOT_ALLOWED");
        }

        if (targetUser.getAuthorities() != null
                && targetUser.getAuthorities().contains("ROLE_ADMIN")
                && isLastAdmin(targetUser)) {
            throw new InvalidUserException("The last admin cannot be deleted.", "LAST_ADMIN_PROTECTED");
        }

        userRepository.delete(targetUser);

        return Records.ApiResponse.success("User deleted successfully.");
    }

    private Records.AdminUserResponse mapAdminUser(UserEntity user) {
        return new Records.AdminUserResponse(
                user.getId() != null ? user.getId().toString() : null,
                user.getEmail(),
                user.getFirst_name(),
                user.getLast_name(),
                user.getAuthorities(),
                user.getCreated_at() != null ? user.getCreated_at().toString() : null
        );
    }
    private boolean isLastAdmin(UserEntity user) {
        long adminCount = userRepository.findAll().stream()
                .filter(existingUser ->
                        existingUser.getAuthorities() != null
                                && existingUser.getAuthorities().contains("ROLE_ADMIN"))
                .count();

        return user.getAuthorities() != null
                && user.getAuthorities().contains("ROLE_ADMIN")
                && adminCount <= 1;
    }

    private String normalizeRole(String role) {
        if (role == null || role.isBlank()) {
            throw new InvalidUserException("Role must not be empty.", "INVALID_ROLE");
        }

        String normalizedRole = role.trim().toUpperCase();

        if (!normalizedRole.startsWith("ROLE_")) {
            normalizedRole = "ROLE_" + normalizedRole;
        }

        return normalizedRole;
    }
    private String normalizeOptionalText(String value) {
        if (value == null) {
            return null;
        }

        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
    private String normalizeRequiredText(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new InvalidUserException(fieldName + " must not be empty.", "INVALID_USER");
        }

        return value.trim();
    }
}

package com.buenws.buenws_backend.API.Service;

import com.buenws.buenws_backend.API.Entity.RefreshTokenEntity;
import com.buenws.buenws_backend.API.Entity.ResetCodeEntity;
import com.buenws.buenws_backend.API.Entity.UserEntity;
import com.buenws.buenws_backend.API.Exception.Custom.*;
import com.buenws.buenws_backend.API.Records.Records;
import com.buenws.buenws_backend.API.Repository.Repositories.RefreshTokenRepository;
import com.buenws.buenws_backend.API.Repository.RepositoryRetrieval;
import com.buenws.buenws_backend.API.Repository.Repositories.ResetCodeRepository;
import com.buenws.buenws_backend.API.Repository.Repositories.UserRepository;
import com.buenws.buenws_backend.API.Service.Tokens.TokenService;
import com.buenws.buenws_backend.Util.CryptographyUtil;
import com.buenws.buenws_backend.Util.MailUtil;
import com.buenws.buenws_backend.Util.TimeUtil;
import com.nimbusds.jose.JOSEException;
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
import java.util.Objects;
import java.util.UUID;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final TokenService tokenService;
    private final AuthenticationManager authenticationManager;
    private final RefreshTokenRepository refreshTokenRepository;
    private final ResetCodeRepository resetCodeRepository;
    private final MailUtil mailUtil;
    private final RepositoryRetrieval repositoryRetrieval;

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder, TokenService tokenService,
                       AuthenticationManager authenticationManager, RefreshTokenRepository refreshTokenRepository,
                       ResetCodeRepository resetCodeRepository, MailUtil mailUtil, RepositoryRetrieval repositoryRetrieval) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.tokenService = tokenService;
        this.authenticationManager = authenticationManager;
        this.refreshTokenRepository = refreshTokenRepository;
        this.resetCodeRepository = resetCodeRepository;
        this.mailUtil = mailUtil;
        this.repositoryRetrieval = repositoryRetrieval;
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
    public Records.ApiResponse<Records.AdminUserResponse> updateUserRole(
            UUID userId,
            Records.AdminUpdateRoleRequest request
    ) {
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
    public Records.ApiResponse<Records.AdminUserResponse> updateUserProfile(
            UUID userId,
            Records.AdminUpdateUserProfileRequest request
    ) {
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

        String currentAdminEmail = SecurityContextHolder.getContext().getAuthentication().getName();

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

    @Transactional
    public Records.ApiResponse<Records.SuccessfulAuthResponse> RegisterUserWithCredentials(
            Records.CredentialsSubmitRequest credentialsSubmitRequest) {
        UserEntity user = new UserEntity();

        user.setFirst_name(credentialsSubmitRequest.first_name());
        user.setLast_name(credentialsSubmitRequest.last_name());

        user.setAuthorities(new ArrayList<>(List.of("ROLE_USER")));
        user.setEmail(credentialsSubmitRequest.email());
        user.setPassword(passwordEncoder.encode(credentialsSubmitRequest.password()));
        user.setRefreshTokenEntity(
                new RefreshTokenEntity(
                        tokenService.generateRefreshToken(),
                        TimeUtil.getCurrentTime(),
                        TimeUtil.getWeekFromNow(),
                        user));
        user.setResetCodeEntity(
                new ResetCodeEntity(
                        user));
        try {
            userRepository.save(user);
        } catch (Exception e) {
            if (e.getMessage().toUpperCase().contains("DUPLICATE KEY VALUE")) {
                throw new DuplicateUserException(
                        "User with Email: '" + credentialsSubmitRequest.email() + "' already exists", "DUPLICATE_USER");
            } else {
                throw new InvalidUserException("Could not create user: " + credentialsSubmitRequest.email(),
                        "INVALID_USER");
            }
        }

        String JWT;
        try {
            JWT = tokenService.generateJWTToken(user);
        } catch (JOSEException e) {
            throw new GenerateTokenException("Error login User in. Please try again.", "GENERATE_TOKEN_ERROR");
        }

        return Records.ApiResponse.success(
                "User with Email: '" + credentialsSubmitRequest.email() + "' registered successfully",
                new Records.SuccessfulAuthResponse(
                        JWT,
                        user.getRefreshTokenEntity().getToken()));
    }

    @Transactional
    public Records.ApiResponse<Records.SuccessfulAuthResponse> LoginUserWithCredentials(
            Records.CredentialsSubmitRequest credentialsSubmitRequest) {

        try {
            Authentication authenticationRequest = UsernamePasswordAuthenticationToken.unauthenticated(
                    credentialsSubmitRequest.email(),
                    credentialsSubmitRequest.password());

            authenticationManager.authenticate(authenticationRequest);

            UserEntity user = repositoryRetrieval.getUserEntityFromEmail(credentialsSubmitRequest.email());

            String JWTToken;
            try {
                JWTToken = tokenService.generateJWTToken(user);
            } catch (JOSEException e) {
                throw new GenerateTokenException("Error login User in. Please try again.", "GENERATE_TOKEN_ERROR");
            }

            String refreshToken = tokenService.generateRefreshToken();

            user.getRefreshTokenEntity().setToken(refreshToken);
            userRepository.save(user);

            return Records.ApiResponse.success(
                    "Log in was successful.",
                    new Records.SuccessfulAuthResponse(
                            JWTToken,
                            refreshToken));

        } catch (AuthenticationException e) {
            throw new InvalidUserException("Invalid email or password.", "INVALID_CREDENTIALS");
        }
    }

    @Transactional
    public Records.ApiResponse<Records.SuccessfulAuthResponse> RefreshToken(
            Records.RefreshTokenRequest refreshTokenRequest) {

        RefreshTokenEntity refreshTokenEntity;
        String JWTToken;
        UserEntity userEntity;

        try {
            refreshTokenEntity = tokenService.validateRefreshToken(refreshTokenRequest.refresh_token());
            userEntity = refreshTokenEntity.getUserEntity();
            JWTToken = tokenService.generateJWTToken(userEntity);
        } catch (ParseException | JOSEException exception) {
            throw new ParseTokenException("Please Log in again.", "INVALID_TOKEN");
        }

        String RefreshToken = tokenService.generateRefreshToken();

        refreshTokenEntity.setToken(RefreshToken);
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
    public Records.ApiResponse<Void> InitChangePassword(Records.InitResetPasswordRequest initResetPasswordRequest) {
        UserEntity user = repositoryRetrieval.getUserEntityFromEmail(initResetPasswordRequest.email());
        ResetCodeEntity resetCodeEntity = user.getResetCodeEntity();

        String plainOTP = CryptographyUtil.generateOTP();

        resetCodeEntity.setReset_code(CryptographyUtil.HashString(plainOTP));
        resetCodeEntity.setActive(true);
        resetCodeEntity.setUpdated_at(TimeUtil.getCurrentTime());
        resetCodeEntity.setExpires_at(TimeUtil.get15MinutesFromNow());

        mailUtil.SendOTPMail(initResetPasswordRequest.email(), "Confirming Identity to change your Password.",
                plainOTP);

        resetCodeRepository.save(resetCodeEntity);

        return Records.ApiResponse.success(
                "If an account exists, an email to reset your password has been sent.");
    }

    @Transactional
    public Records.ApiResponse<Records.VerifyOTPResponse> VerifyOTP(Records.VerifyOTPRequest verifyOTPRequest) {
        UserEntity user = repositoryRetrieval.getUserEntityFromEmail(verifyOTPRequest.email());
        ResetCodeEntity resetCodeEntity = user.getResetCodeEntity();
        int currentAttempts = resetCodeEntity.getAttempts();

        if (TimeUtil.getCurrentTime().isBefore(resetCodeEntity.getExpires_at()) && currentAttempts < 4) {
            resetCodeEntity.setAttempts(currentAttempts + 1);

            if (Objects.equals(resetCodeEntity.getReset_code(), CryptographyUtil.HashString(verifyOTPRequest.otp()))
                    && resetCodeEntity.getActive()) {
                String verified_token = UUID.randomUUID().toString();
                resetCodeEntity.setVerified_token(verified_token);

                resetCodeRepository.save(resetCodeEntity);
                return Records.ApiResponse.success("OTP verified.", new Records.VerifyOTPResponse(
                        verified_token));
            }
        } else {
            resetCodeEntity.setActive(false);
        }

        resetCodeRepository.save(resetCodeEntity);
        throw new ResetPasswordException("Couldn't verify Validity of OTP ", "INVALID_OTP");
    }

    @Transactional
    public Records.ApiResponse<Void> ChangePassword(Records.ChangePasswordRequest changePasswordRequest) {
        ResetCodeEntity resetCodeEntity = repositoryRetrieval
                .getResetCodeEntityFromVerified_Token(changePasswordRequest.verified_token());
        UserEntity user = resetCodeEntity.getUserEntity();
        user.setPassword(passwordEncoder.encode(changePasswordRequest.password()));

        resetCodeEntity.setActive(false);
        resetCodeEntity.setVerified_token("");
        resetCodeEntity.setExpires_at(TimeUtil.getCurrentTime());
        resetCodeEntity.setAttempts(0);

        resetCodeRepository.save(resetCodeEntity);
        userRepository.save(user);

        return Records.ApiResponse.success("Password was successfully changed.");
    }
}
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
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.ParseException;
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

    @Value("${hashing.salt}")
    String salt;

    @Transactional
    public Records.ApiResponse<Records.SuccessfulAuthResponse> RegisterUserWithCredentials(
            Records.CredentialsSubmitRequest credentialsSubmitRequest) {
        UserEntity user = new UserEntity();

        user.setFirst_name(credentialsSubmitRequest.first_name());
        user.setLast_name(credentialsSubmitRequest.last_name());

        user.setAuthorities(List.of("ROLE_USER"));
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
        } catch (DuplicateKeyException e) {
                throw new DuplicateUserException(
                        "User with Email: '" + credentialsSubmitRequest.email() + "' already exists", "DUPLICATE_USER");
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

    // Login Logic
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

            userRepository.save(user);

            return Records.ApiResponse.success(
                    "Log in was successful.",
                    new Records.SuccessfulAuthResponse(
                            JWTToken,
                            user.getRefreshTokenEntity().getToken()
                    )
            );

        } catch (AuthenticationException e) {
            throw new InvalidUserException("Invalid email or password.", "INVALID_CREDENTIALS");
        }
    }

    // RefreshToken Logic
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

    // Reset Password Logic
    @Transactional
    public Records.ApiResponse<Void> InitChangePassword(Records.InitResetPasswordRequest initResetPasswordRequest) {
        try{
            UserEntity user = repositoryRetrieval.getUserEntityFromEmail(initResetPasswordRequest.email());

            ResetCodeEntity resetCodeEntity = user.getResetCodeEntity();

            String plainOTP = CryptographyUtil.generateOTP();

            resetCodeEntity.setReset_code(CryptographyUtil.HashString(plainOTP, salt));
            resetCodeEntity.setActive(true);
            resetCodeEntity.setUpdated_at(TimeUtil.getCurrentTime());
            resetCodeEntity.setExpires_at(TimeUtil.get15MinutesFromNow());

            mailUtil.SendOTPMail(initResetPasswordRequest.email(), "Confirming Identity to change your Password.",
                    plainOTP);

            resetCodeRepository.save(resetCodeEntity);
        }catch (InvalidUserException e){}

        return Records.ApiResponse.success(
                "If an account exists, an email to reset your password has been sent."
        );
    }

    @Transactional(noRollbackFor = ResetPasswordException.class)
    public Records.ApiResponse<Records.VerifyOTPResponse> VerifyOTP(Records.VerifyOTPRequest verifyOTPRequest) {
        UserEntity user = repositoryRetrieval.getUserEntityFromEmail(verifyOTPRequest.email());
        ResetCodeEntity resetCodeEntity = user.getResetCodeEntity();
        int currentAttempts = resetCodeEntity.getAttempts();

        if (TimeUtil.getCurrentTime().isBefore(resetCodeEntity.getExpires_at()) && currentAttempts < 4) {
            resetCodeEntity.setAttempts(currentAttempts + 1);

            if (Objects.equals(resetCodeEntity.getReset_code(), CryptographyUtil.HashString(verifyOTPRequest.otp(), salt))
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
        if(changePasswordRequest.verified_token() != null && !changePasswordRequest.verified_token().isEmpty()){
            ResetCodeEntity resetCodeEntity = repositoryRetrieval.getResetCodeEntityFromVerified_Token(changePasswordRequest.verified_token());

            if (TimeUtil.getCurrentTime().isBefore(resetCodeEntity.getExpires_at())){
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
        throw new ResetPasswordException("Password couldn't be changed", "INVALID_OTP");
    }
}

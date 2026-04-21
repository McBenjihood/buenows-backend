package com.buenws.buenws_backend.API.Service;

import com.buenws.buenws_backend.API.Entity.RefreshTokenEntity;
import com.buenws.buenws_backend.API.Entity.ResetCodeEntity;
import com.buenws.buenws_backend.API.Entity.UserEntity;
import com.buenws.buenws_backend.API.Exception.Custom.*;
import com.buenws.buenws_backend.API.Records.Records;
import com.buenws.buenws_backend.API.Repository.RefreshTokenRepository;
import com.buenws.buenws_backend.API.Repository.ResetCodeRepository;
import com.buenws.buenws_backend.API.Repository.UserRepository;
import com.buenws.buenws_backend.API.Service.Tokens.TokenService;
import com.buenws.buenws_backend.Util.CryptographyUtil;
import com.buenws.buenws_backend.Util.TimeUtil;
import com.nimbusds.jose.JOSEException;
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
import java.util.Optional;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final TokenService tokenService;
    private final AuthenticationManager authenticationManager;
    private final RefreshTokenRepository refreshTokenRepository;
    private final ResetCodeRepository resetCodeEntityRepository;

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder, TokenService tokenService, AuthenticationManager authenticationManager, RefreshTokenRepository refreshTokenRepository, ResetCodeRepository resetCodeEntityRepository) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.tokenService = tokenService;
        this.authenticationManager = authenticationManager;
        this.refreshTokenRepository = refreshTokenRepository;
        this.resetCodeEntityRepository = resetCodeEntityRepository;
    }

    public UserEntity getUserEntityFromToken(String token){
        com.nimbusds.jwt.JWTClaimsSet claimsSet = tokenService.validateJWTToken(token);
        Optional<UserEntity> userEntity = userRepository.findByEmail(claimsSet.getSubject());
        if(userEntity.isPresent()){
            return userEntity.get();
        }else {
            throw new InvalidUserException("User not found.", "INVALID_USER");
        }
    }
    public UserEntity getUserEntityFromEmail(String email){
        Optional<UserEntity> userEntity = userRepository.findByEmail(email);
        if(userEntity.isPresent()){
            return userEntity.get();
        }else {
            throw new InvalidUserException("User not found.", "INVALID_USER");
        }
    }

    //Register Logic
    @Transactional
    public Records.ApiResponse<Records.SuccessfulAuthResponse> RegisterUserWithCredentials(Records.CredentialsSubmitRequest credentialsSubmitRequest) {
        UserEntity user = new UserEntity();

        user.setAuthorities(List.of("ROLE_USER"));
        user.setEmail(credentialsSubmitRequest.email());
        user.setPassword(passwordEncoder.encode(credentialsSubmitRequest.password()));
        user.setRefreshTokenEntity(
                new RefreshTokenEntity(
                        tokenService.generateRefreshToken(),
                        TimeUtil.getCurrentDate(),
                        TimeUtil.getWeekFromNow(),
                        user
                ));
        user.setResetCodeEntity(
            new ResetCodeEntity(
                    user
            )
        );
        try {
            userRepository.save(user);
        } catch (Exception e) {
            if (e.getMessage().toUpperCase().contains("DUPLICATE KEY VALUE")){
                throw new DuplicateUserException("User with Email: '" + credentialsSubmitRequest.email() + "' already exists", "DUPLICATE_USER");
            }else {
                throw new InvalidUserException("Could not create user: " + credentialsSubmitRequest.email(), "INVALID_USER");
            }
        }

        String JWT;
        try{
            JWT = tokenService.generateJWTToken(user);
        } catch (JOSEException e){
            throw new GenerateTokenException("Error login User in. Please try again.", "GENERATE_TOKEN_ERROR");
        }

        return Records.ApiResponse.success(
                "User with Email: '" + credentialsSubmitRequest.email() + "' registered successfully",
                new Records.SuccessfulAuthResponse(
                        JWT,
                        user.getRefreshTokenEntity().getToken()
                )
        );
    }

    //Login Logic
    @Transactional
    public Records.ApiResponse<Records.SuccessfulAuthResponse> LoginUserWithCredentials(Records.CredentialsSubmitRequest credentialsSubmitRequest) {

        try {
            Authentication authenticationRequest = UsernamePasswordAuthenticationToken.unauthenticated(
                    credentialsSubmitRequest.email(),
                    credentialsSubmitRequest.password()
            );

            authenticationManager.authenticate(authenticationRequest);

            UserEntity user = getUserEntityFromEmail(credentialsSubmitRequest.email());

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
                            refreshToken
                    )
            );

        } catch (AuthenticationException e) {
            throw new InvalidUserException("Invalid email or password.", "INVALID_CREDENTIALS");
        }
    }

    //RefreshToken Logic
    @Transactional
    public Records.ApiResponse<Records.SuccessfulAuthResponse> RefreshToken(Records.RefreshTokenRequest refreshTokenRequest){

        RefreshTokenEntity refreshTokenEntity;
        String JWTToken;
        UserEntity userEntity;

        try {
            refreshTokenEntity = tokenService.validateRefreshToken(refreshTokenRequest.refresh_token());
            userEntity = refreshTokenEntity.getUserEntity();
            JWTToken =  tokenService.generateJWTToken(userEntity);
        }catch (ParseException | JOSEException exception){
            throw new ParseTokenException("Please Log in again.", "INVALID_TOKEN");
        }

        String RefreshToken = tokenService.generateRefreshToken();

        refreshTokenEntity.setToken(RefreshToken);
        refreshTokenEntity.setEdited_at(TimeUtil.getCurrentDate());
        refreshTokenEntity.setExpires_at(TimeUtil.getWeekFromNow());

        refreshTokenRepository.save(refreshTokenEntity);

        return Records.ApiResponse.success(
                "Tokens generated successfully.",
                new Records.SuccessfulAuthResponse(
                        JWTToken,
                        RefreshToken
                )
        );
    }

    //Reset Password Logic
    public Records.ApiResponse<Void> InitChangePassword(Records.InitResetPasswordRequest initResetPasswordRequest){
        UserEntity user = getUserEntityFromEmail(initResetPasswordRequest.email());
        ResetCodeEntity resetCodeEntity = user.getResetCodeEntity();

        resetCodeEntity.setReset_code(CryptographyUtil.HashString(CryptographyUtil.generateOTP()));
        resetCodeEntity.setActive(true);
        resetCodeEntity.setUpdated_at(TimeUtil.getCurrentDate());
        resetCodeEntity.setExpires_at(TimeUtil.get15MinutesFromNow());

        resetCodeEntityRepository.save(resetCodeEntity);

        return Records.ApiResponse.success(
                "If an account exists, an email to reset your password has been sent."
        );
    }

    public Records.ApiResponse<Void> VerifyOTPChangePassword(Records.VerifyOTPResetPasswordRequest verifyOTPResetPasswordRequest){
        UserEntity user = getUserEntityFromEmail(verifyOTPResetPasswordRequest.email());
        ResetCodeEntity resetCodeEntity = user.getResetCodeEntity();

        if (resetCodeEntity.getActive() && TimeUtil.getCurrentDate().isBefore(resetCodeEntity.getExpires_at())){
            if(Objects.equals(resetCodeEntity.getReset_code(), CryptographyUtil.HashString(verifyOTPResetPasswordRequest.otp()))){
                user.setPassword(passwordEncoder.encode(verifyOTPResetPasswordRequest.password()));
                return Records.ApiResponse.success("Password was successfully changed.");
            }else {
                throw new OTPException("The entered Code was not valid.", "INVALID_OTP");
            }
        }else{
            throw new ResetPasswordException("Couldn't change Password for User with this Email", "INVALID_USER");
        }
    }
}

package com.buenws.buenws_backend.API.Service;

import com.buenws.buenws_backend.API.Entity.RefreshTokenEntity;
import com.buenws.buenws_backend.API.Entity.ResetCodeEntity;
import com.buenws.buenws_backend.API.Entity.UserEntity;
import com.buenws.buenws_backend.API.Exception.Custom.DuplicateUserException;
import com.buenws.buenws_backend.API.Exception.Custom.GenerateTokenException;
import com.buenws.buenws_backend.API.Exception.Custom.InvalidUserException;
import com.buenws.buenws_backend.API.Exception.Custom.ParseTokenException;
import com.buenws.buenws_backend.API.Records.UserRecords;
import com.buenws.buenws_backend.API.Repository.UserRepository;
import com.buenws.buenws_backend.API.Service.Tokens.TokenService;
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
import java.util.Optional;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final TokenService tokenService;
    private final AuthenticationManager authenticationManager;

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder, TokenService tokenService, AuthenticationManager authenticationManager) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.tokenService = tokenService;
        this.authenticationManager = authenticationManager;
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
    public UserRecords.ApiResponse<UserRecords.SuccessfulAuthResponse> RegisterUserWithCredentials(UserRecords.CredentialsSubmitRequest credentialsSubmitRequest) {
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
                    TimeUtil.getCurrentDate(),
                    TimeUtil.get15MinutesFromNow()
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

        return UserRecords.ApiResponse.success(
                "User with Email: '" + credentialsSubmitRequest.email() + "' registered successfully",
                new UserRecords.SuccessfulAuthResponse(
                        JWT,
                        user.getRefreshTokenEntity().getToken()
                )
        );
    }

    //Login Logic
    @Transactional
    public UserRecords.ApiResponse<UserRecords.SuccessfulAuthResponse> LoginUserWithCredentials(UserRecords.CredentialsSubmitRequest credentialsSubmitRequest) {

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

            return UserRecords.ApiResponse.success(
                    "Log in was successful.",
                    new UserRecords.SuccessfulAuthResponse(
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
    public UserRecords.ApiResponse<UserRecords.SuccessfulAuthResponse> RefreshToken(UserRecords.RefreshTokenRequest refreshTokenRequest){

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

        userRepository.save(userEntity);

        return UserRecords.ApiResponse.success(
                "Tokens generated successfully.",
                new UserRecords.SuccessfulAuthResponse(
                        JWTToken,
                        RefreshToken
                )
        );
    }

    //Reset Password Logic
    public UserRecords.ApiResponse<Void> ChangePassword(UserRecords.ResetPasswordRequest resetPasswordRequest){
        UserEntity user = getUserEntityFromEmail(resetPasswordRequest.email());
        ResetCodeEntity resetCodeEntity = user.getResetCodeEntity();



        return UserRecords.ApiResponse.success(
                "If an account exists, an email to reset your password has been sent."
        );
    }
}

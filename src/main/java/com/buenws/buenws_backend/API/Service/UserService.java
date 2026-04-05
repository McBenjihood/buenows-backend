package com.buenws.buenws_backend.API.Service;

import com.buenws.buenws_backend.API.Entity.RefreshTokenEntity;
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
    public UserRecords.ApiResponse<UserRecords.SuccessfulAuthResponseRecord> RegisterUserWithCredentials(UserRecords.CredentialsSubmitRequestRecord credentialsSubmitRequestRecord) {
        UserEntity user = new UserEntity();

        user.setAuthorities(List.of("ROLE_USER"));
        user.setEmail(credentialsSubmitRequestRecord.email());
        user.setPassword(passwordEncoder.encode(credentialsSubmitRequestRecord.password()));
        user.setRefreshTokenEntity(
                new RefreshTokenEntity(
                        tokenService.generateRefreshToken(),
                        TimeUtil.getCurrentDate(),
                        TimeUtil.getWeekFromNow(),
                        user
                ));

        try {
            userRepository.save(user);
        } catch (Exception e) {
            if (e.getMessage().toUpperCase().contains("DUPLICATE KEY VALUE")){
                throw new DuplicateUserException("User with Email: '" + credentialsSubmitRequestRecord.email() + "' already exists", "DUPLICATE_USER");
            }else {
                throw new InvalidUserException("Could not create user: " + credentialsSubmitRequestRecord.email(), "INVALID_USER");
            }
        }

        String JWT;
        try{
            JWT = tokenService.generateJWTToken(user);
        } catch (JOSEException e){
            throw new GenerateTokenException("Error login User in. Please try again.", "GENERATE_TOKEN_ERROR");
        }

        return UserRecords.ApiResponse.success(
                "User with Email: '" + credentialsSubmitRequestRecord.email() + "' registered successfully",
                new UserRecords.SuccessfulAuthResponseRecord(
                        JWT,
                        user.getRefreshTokenEntity().getToken()
                )
        );
    }

    //Login Logic
    @Transactional
    public UserRecords.ApiResponse<UserRecords.SuccessfulAuthResponseRecord> LoginUserWithCredentials(
            UserRecords.CredentialsSubmitRequestRecord credentialsSubmitRequestRecord) {

        try {
            Authentication authenticationRequest = UsernamePasswordAuthenticationToken.unauthenticated(
                    credentialsSubmitRequestRecord.email(),
                    credentialsSubmitRequestRecord.password()
            );

            authenticationManager.authenticate(authenticationRequest);

            UserEntity user = getUserEntityFromEmail(credentialsSubmitRequestRecord.email());

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
                    new UserRecords.SuccessfulAuthResponseRecord(
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
    public UserRecords.ApiResponse<UserRecords.SuccessfulAuthResponseRecord> RefreshTokens (UserRecords.RefreshTokenRequestRecord refreshTokenRequestRecord){

        RefreshTokenEntity refreshTokenEntity;
        String JWTToken;
        UserEntity userEntity;

        try {
            refreshTokenEntity = tokenService.validateRefreshToken(refreshTokenRequestRecord.refresh_token());
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
                new UserRecords.SuccessfulAuthResponseRecord(
                        JWTToken,
                        RefreshToken
                )
        );
    }
}

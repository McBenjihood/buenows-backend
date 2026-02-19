package com.buenws.buenws_backend.api.service;

import com.buenws.buenws_backend.api.entity.RefreshTokenEntity;
import com.buenws.buenws_backend.api.entity.UserEntity;
import com.buenws.buenws_backend.api.exception.customExceptions.DuplicateUserException;
import com.buenws.buenws_backend.api.exception.customExceptions.GenerateTokenException;
import com.buenws.buenws_backend.api.exception.customExceptions.InvalidUserException;
import com.buenws.buenws_backend.api.exception.customExceptions.ParseTokenException;
import com.buenws.buenws_backend.api.records.UserRecords;
import com.buenws.buenws_backend.api.repository.UserRepository;
import com.buenws.buenws_backend.api.service.tokens.TokenService;
import com.buenws.buenws_backend.util.BuenowsUtil;
import com.nimbusds.jose.JOSEException;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.ParseException;
import java.util.List;
import java.util.Optional;

import static org.springframework.http.ResponseEntity.ok;

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

    //Register Logic
    @Transactional
    public UserRecords.ApiResponse registerUser(UserRecords.CredentialsSubmitRequestRecord registerRequestRecord) {
        UserEntity userEntity = new UserEntity();

        userEntity.setAuthorities(List.of("ROLE_USER"));
        userEntity.setEmail(registerRequestRecord.email());
        userEntity.setPassword(passwordEncoder.encode(registerRequestRecord.password()));
        userEntity.setRefreshTokenEntity(
                new RefreshTokenEntity(
                        tokenService.generateRefreshToken(),
                        BuenowsUtil.getCurrentDate(),
                        BuenowsUtil.getWeekFromNow(),
                        userEntity
                ));

        try {
            userRepository.save(userEntity);
        } catch (Exception e) {
            if (e.getMessage().toUpperCase().contains("DUPLICATE KEY VALUE")){
                throw new DuplicateUserException("User with Email: '" + registerRequestRecord.email() + "' already exists", "DUPLICATE_USER");
            }else {
                throw new InvalidUserException("Could not create user: " + registerRequestRecord.email(), "INVALID_USER");
            }
        }

        return UserRecords.ApiResponse.success("User with Email: '" + registerRequestRecord.email() + "' registered successfully");
    }

    //Login Logic
    @Transactional
    public UserRecords.ApiResponse loginUser(UserRecords.CredentialsSubmitRequestRecord credentialsSubmitRequestRecord) {

            Authentication authenticationRequest = UsernamePasswordAuthenticationToken.unauthenticated(
                    credentialsSubmitRequestRecord.email(),
                    credentialsSubmitRequestRecord.password()
            );
            authenticationManager.authenticate(authenticationRequest);

            Optional<UserEntity> user = userRepository.findByEmail(credentialsSubmitRequestRecord.email());
            if (user.isPresent()){
                UserEntity userEntity = user.get();

                String JWTToken;
                try{
                    JWTToken = tokenService.generateJWTToken(userEntity);
                } catch (JOSEException e){
                    throw new GenerateTokenException("Error login User in. Please try again.", "GENERATE_TOKEN_ERROR");
                }

                String RefreshToken = tokenService.generateRefreshToken();

                userEntity.getRefreshTokenEntity().setToken(RefreshToken);
                userRepository.save(userEntity);

                return UserRecords.ApiResponse.success(
                        "Log in was successful.",
                        new UserRecords.LoginResponseRecord(
                                JWTToken,
                                RefreshToken,
                                userEntity.getEmail()
                        )
                );
            }
            else {
                throw new InvalidUserException("User with these Credentials could not be found.", "INVALID_USER");
            }
    }

    //RefreshToken Logic
    @Transactional
    public UserRecords.ApiResponse refreshToken (UserRecords.RefreshTokenRequestRecord refreshTokenRequestRecord){

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
        refreshTokenEntity.setEdited_at(BuenowsUtil.getCurrentDate());
        refreshTokenEntity.setExpires_at(BuenowsUtil.getWeekFromNow());

        userRepository.save(userEntity);

        return UserRecords.ApiResponse.success(
                "Tokens generated successfully.",
                new UserRecords.RefreshTokenResponseRecord(
                        JWTToken,
                        RefreshToken
                )
        );
    }
}

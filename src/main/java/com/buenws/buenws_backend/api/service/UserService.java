package com.buenws.buenws_backend.api.service;

import com.buenws.buenws_backend.api.entity.RefreshTokenEntity;
import com.buenws.buenws_backend.api.entity.UserEntity;
import com.buenws.buenws_backend.api.exception.customExceptions.CouldNotCreateResourceException;
import com.buenws.buenws_backend.api.exception.customExceptions.ParseTokenException;
import com.buenws.buenws_backend.api.exception.customExceptions.UserNotFoundException;
import com.buenws.buenws_backend.api.records.UserRecords;
import com.buenws.buenws_backend.api.repository.RefreshTokenRepository;
import com.buenws.buenws_backend.api.repository.UserRepository;
import com.buenws.buenws_backend.api.service.tokens.TokenService;
import com.buenws.buenws_backend.util.BuenowsUtil;
import com.nimbusds.jose.JOSEException;
import org.springframework.http.ResponseEntity;
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
    private final RefreshTokenRepository refreshTokenRepository;

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder, TokenService tokenService, AuthenticationManager authenticationManager, RefreshTokenRepository refreshTokenRepository) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.tokenService = tokenService;
        this.authenticationManager = authenticationManager;
        this.refreshTokenRepository = refreshTokenRepository;
    }

    //Register Logic
    @Transactional
    public UserRecords.RegisterResponseRecord registerUser(UserRecords.CredentialsSubmitRequestRecord registerRequestRecord) {
        try {
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

            userRepository.save(userEntity);
        } catch (Exception e) {
            if (e.getMessage().toUpperCase().contains("DUPLICATE KEY VALUE")){
                throw new CouldNotCreateResourceException("User with Email: '" + registerRequestRecord.email() + "' already exists");
            }else {
                throw new CouldNotCreateResourceException("Could not create user: " + registerRequestRecord.email());
            }
        }
        return new UserRecords.RegisterResponseRecord(true, "User created successfully");
    }

    //Login Logic
    public ResponseEntity<UserRecords.LoginResponseRecord> loginUser(UserRecords.CredentialsSubmitRequestRecord credentialsSubmitRequestRecord) {
        try{
            Authentication authenticationRequest = UsernamePasswordAuthenticationToken.unauthenticated(
                    credentialsSubmitRequestRecord.email(),
                    credentialsSubmitRequestRecord.password()
            );
            authenticationManager.authenticate(authenticationRequest);

            Optional<UserEntity> user = userRepository.findByEmail(credentialsSubmitRequestRecord.email());

            if (user.isPresent()){
                UserEntity userEntity = user.get();

                String JWTToken = tokenService.generateJWTToken(userEntity);
                String RefreshToken = tokenService.generateRefreshToken();

                userEntity.getRefreshTokenEntity().setToken(RefreshToken);
                userRepository.save(userEntity);

                return ResponseEntity
                        .ok()
                        .body(
                                new UserRecords.LoginResponseRecord(
                                        true,
                                        "Login was Successful",
                                        "Bearer",
                                        JWTToken,
                                        RefreshToken,
                                        userEntity.getEmail()
                        )
                );
            }
            else {
                throw new UserNotFoundException("User not found.");
            }

        }catch (JOSEException e){
            throw new ParseTokenException("Error processing login token.");
        }
    }

    //RefreshToken Logic
    @Transactional
    public UserRecords.RefreshTokenResponseRecord refreshToken (UserRecords.RefreshTokenRequestRecord refreshTokenRequestRecord){
        try {
            RefreshTokenEntity refreshTokenEntity = tokenService.validateRefreshToken(refreshTokenRequestRecord.refresh_token());
            UserEntity userEntity = refreshTokenEntity.getUserEntity();

            String JWTToken =  tokenService.generateJWTToken(userEntity);
            String RefreshToken = tokenService.generateRefreshToken();

            refreshTokenEntity.setToken(RefreshToken);
            refreshTokenEntity.setEdited_at(BuenowsUtil.getCurrentDate());
            refreshTokenEntity.setExpires_at(BuenowsUtil.getWeekFromNow());

            userRepository.save(userEntity);

            return new UserRecords.RefreshTokenResponseRecord(
                    true,
                    "New Tokens successfully generated.",
                    JWTToken,
                    RefreshToken
            );
        }catch (ParseException | JOSEException exception){
            throw new ParseTokenException("Error processing login token.");
        }
    }
}

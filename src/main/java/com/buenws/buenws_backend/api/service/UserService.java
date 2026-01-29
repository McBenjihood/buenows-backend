package com.buenws.buenws_backend.api.service;

import com.buenws.buenws_backend.api.entity.UserEntity;
import com.buenws.buenws_backend.api.exception.customExceptions.CouldNotCreateResourceException;
import com.buenws.buenws_backend.api.exception.customExceptions.ParseTokenException;
import com.buenws.buenws_backend.api.exception.customExceptions.UserNotFoundException;
import com.buenws.buenws_backend.api.records.UserRecords;
import com.buenws.buenws_backend.api.repository.UserRepository;
import com.buenws.buenws_backend.api.service.tokens.TokenService;
import com.nimbusds.jose.JOSEException;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
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
        this.tokenService = tokenService;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
    }

    //Register Logic
    @Transactional
    public UserRecords.RegisterResponseRecord registerUser(UserRecords.CredentialsSubmitRequestRecord registerRequestRecord) {
        try {
            UserEntity userEntity = new UserEntity();

            userEntity.setAuthorities(List.of("ROLE_USER"));
            userEntity.setEmail(registerRequestRecord.email());
            userEntity.setPassword(passwordEncoder.encode(registerRequestRecord.password()));

            System.out.println("Trying to create User...");
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

            String token = tokenService
                .generateToken(
                    user.get()
                );
            return ResponseEntity.ok(
                new UserRecords.LoginResponseRecord(
                true,
                        "Login was succesfull",
                        "Bearer",
                        token,
                        tokenService.getExpirationFromToken(token).getTime(),
                        credentialsSubmitRequestRecord.email()
                )
            );

        }catch (ParseException | JOSEException e){
            throw new ParseTokenException("Error processing login token");
        }
    }

}

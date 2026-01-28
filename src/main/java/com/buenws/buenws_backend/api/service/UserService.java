package com.buenws.buenws_backend.api.service;

import com.buenws.buenws_backend.api.entity.UserEntity;
import com.buenws.buenws_backend.api.exception.customExceptions.CouldNotCreateResourceException;
import com.buenws.buenws_backend.api.records.UserRecords;
import com.buenws.buenws_backend.api.repository.UserRepository;
import com.buenws.buenws_backend.api.service.tokens.TokenService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final TokenService tokenService;

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder, TokenService tokenService) {
        this.tokenService = tokenService;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public UserRecords.CredentialsResponseRecord registerUser(UserRecords.CredentialsSubmitRequestRecord registerRequestRecord) {
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

        return new UserRecords.CredentialsResponseRecord(true, "User created successfully");
    }

    public UserRecords.CredentialsResponseRecord loginUser(UserRecords.CredentialsSubmitRequestRecord credentialsSubmitRequestRecord) {
        try{
            System.out.println(tokenService.generateToken(credentialsSubmitRequestRecord.email()));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        return new UserRecords.CredentialsResponseRecord(true, "User logged in successfully");
    }

}

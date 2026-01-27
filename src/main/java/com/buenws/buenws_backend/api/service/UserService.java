package com.buenws.buenws_backend.api.service;

import com.buenws.buenws_backend.api.entities.UserEntity;
import com.buenws.buenws_backend.api.exception.customExceptions.CouldNotCreateResourceException;
import com.buenws.buenws_backend.api.records.UserRecords;
import com.buenws.buenws_backend.api.repository.InquiryRepository;
import com.buenws.buenws_backend.api.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public UserRecords.RegisterResponseRecord registerUser(UserRecords.RegisterRequestRecord registerRequestRecord) {
        try {
            UserEntity userEntity = new UserEntity();

            userEntity.setAuthorities(List.of("ROLE_USER"));
            userEntity.setEmail(registerRequestRecord.email());
            userEntity.setPassword(passwordEncoder.encode(registerRequestRecord.password()));

            System.out.println("Trying to create User...");
            userRepository.save(userEntity);

        } catch (Exception e) {
            System.out.println(e.getMessage());
            if (e.getMessage().toUpperCase().contains("DUPLICATE KEY VALUE")){
                throw new CouldNotCreateResourceException("User with Email: '" + registerRequestRecord.email() + "' already exists");
            }else {
                throw new CouldNotCreateResourceException("Could not create user: " + registerRequestRecord.email());
            }
        }

        return new UserRecords.RegisterResponseRecord(true, "User created successfully");
    }

}

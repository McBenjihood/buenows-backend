package com.buenws.buenws_backend.API.Configuration;

import com.buenws.buenws_backend.API.Entity.RefreshTokenEntity;
import com.buenws.buenws_backend.API.Entity.UserEntity;
import com.buenws.buenws_backend.API.Repository.Repositories.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;

@Component
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${admin.email}")
    private String adminEmail;

    @Value("${admin.password}")
    private String adminPassword;

    public DataInitializer(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        boolean adminExists = userRepository.findAll()
                .stream()
                .anyMatch(user -> user.getAuthorities() != null &&
                        user.getAuthorities().contains("ROLE_ADMIN"));

        if (!adminExists) {
            UserEntity admin = new UserEntity();
            admin.setEmail(adminEmail);
            admin.setPassword(passwordEncoder.encode(adminPassword));
            admin.setAuthorities(List.of("ROLE_ADMIN"));

            RefreshTokenEntity refreshToken = new RefreshTokenEntity();
            refreshToken.setToken("");
            refreshToken.setEdited_at(Instant.now());
            refreshToken.setExpires_at(Instant.now());
            refreshToken.setUserEntity(admin);

            admin.setRefreshTokenEntity(refreshToken);

            userRepository.save(admin);

            System.out.println("Admin created: " + adminEmail);
        }
    }
}
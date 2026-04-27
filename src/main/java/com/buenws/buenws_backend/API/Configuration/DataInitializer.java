package com.buenws.buenws_backend.API.Configuration;

import com.buenws.buenws_backend.API.Entity.UserEntity;
import com.buenws.buenws_backend.API.Repository.Repositories.UserRepository;import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public DataInitializer(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        boolean adminExists = userRepository.findAll()
                .stream()
                .anyMatch(user -> user.getAuthorities() != null &&
                        user.getAuthorities().contains("ADMIN"));

        if (!adminExists) {
            UserEntity admin = new UserEntity();
            admin.setEmail("admin@buenows.com");
            admin.setPassword(passwordEncoder.encode("admin123"));
            admin.setAuthorities(List.of("ADMIN"));

            userRepository.save(admin);

            System.out.println("Admin created: admin@buenows.com");
        }
    }
}
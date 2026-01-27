package com.buenws.buenws_backend.api.service;

import com.buenws.buenws_backend.api.entities.UserEntity;
import com.buenws.buenws_backend.api.repository.UserRepository;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class CustomUserDetailsService implements UserDetailsService {

    public CustomUserDetailsService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    private final UserRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {

        UserEntity user = userRepository.findByEmail(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found with username: " + username));

        return User.builder()
                .username(user.getEmail())
                .password(user.getPassword())
                .authorities(AuthorityUtils.createAuthorityList(user.getAuthorities().toArray(new String[0])))
                .build();
    }
}

package com.buenws.buenws_backend.API.Configuration;

import com.buenws.buenws_backend.API.Service.CustomUserDetailsService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.security.config.http.SessionCreationPolicy;

import java.util.List;

@Configuration
@EnableWebSecurity
public class SecurityConfiguration {

    private final BearerTokenAuthFilter bearerTokenAuthFilter;

    public SecurityConfiguration(BearerTokenAuthFilter bearerTokenAuthFilter) {
        this.bearerTokenAuthFilter = bearerTokenAuthFilter;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .cors(Customizer.withDefaults())
                .csrf(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .logout(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests((authorize) -> authorize
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()

                        .requestMatchers("/api/inquiry/contact-submissions").permitAll()

                        .requestMatchers("/api/user/auth/register").permitAll()
                        .requestMatchers("/api/user/auth/login").permitAll()
                        .requestMatchers("/api/user/auth/refresh").permitAll()
                        .requestMatchers("/api/user/auth/logout").permitAll()

                        .requestMatchers("/api/user/request-otp").permitAll()
                        .requestMatchers("/api/user/verify-otp").permitAll()
                        .requestMatchers("/api/user/change-password").permitAll()

                        .requestMatchers("/images/**").permitAll()

                        .requestMatchers(HttpMethod.GET, "/api/admin/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/api/admin/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/api/admin/**").hasRole("ADMIN")

                        .requestMatchers("/api/user/auth").authenticated()
                        .requestMatchers("/api/user/**").hasAnyRole("USER", "ADMIN")

                        .anyRequest().authenticated()
                )
                .addFilterAfter(bearerTokenAuthFilter, BasicAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();

        config.setAllowedOriginPatterns(List.of("http://localhost:*"));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        config.setExposedHeaders(List.of("*"));
        config.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return PasswordEncoderFactories.createDelegatingPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(
            CustomUserDetailsService customUserDetailsService,
            PasswordEncoder passwordEncoder
    ) {
        DaoAuthenticationProvider authenticationProvider = new DaoAuthenticationProvider(customUserDetailsService);
        authenticationProvider.setPasswordEncoder(passwordEncoder);

        return new ProviderManager(authenticationProvider);
    }
}
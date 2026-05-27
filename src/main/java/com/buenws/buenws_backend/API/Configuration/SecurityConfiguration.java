package com.buenws.buenws_backend.API.Configuration;

import com.buenws.buenws_backend.API.Service.CustomUserDetailsService;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
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
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.security.config.http.SessionCreationPolicy;

import java.util.Arrays;
import java.util.List;

@Configuration
@EnableWebSecurity
public class SecurityConfiguration {

    private final BearerTokenAuthFilter bearerTokenAuthFilter;

    @Value("${app.cors.allowed-origins}")
    private String allowedOrigins;

    @Value("${app.security.require-https:false}")
    private boolean requireHttps;

    public SecurityConfiguration(BearerTokenAuthFilter bearerTokenAuthFilter) {
        this.bearerTokenAuthFilter = bearerTokenAuthFilter;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        CookieCsrfTokenRepository csrfTokenRepository = CookieCsrfTokenRepository.withHttpOnlyFalse();
        csrfTokenRepository.setCookiePath("/");
        csrfTokenRepository.setCookieCustomizer(cookie -> cookie
                .secure(true)
                .sameSite("Lax")
        );

        http
                .cors(Customizer.withDefaults())
                .csrf(csrf -> csrf
                        .csrfTokenRepository(csrfTokenRepository)
                        .ignoringRequestMatchers(
                                "/api/admin/inquiries",
                                "/api/admin/inquiries/delete",
                                "/api/admin/test",
                                "/api/admin/users",
                                "/api/admin/users/delete",
                                "/api/admin/users/update-profile",
                                "/api/admin/users/update-role",
                                "/api/inquiry/contact-submissions",
                                "/api/user/auth/login",
                                "/api/user/auth/register",
                                "/api/user/change-password",
                                "/api/user/request-otp",
                                "/api/user/verify-otp"
                        )
                )
                .httpBasic(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .logout(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .exceptionHandling(exception -> exception.authenticationEntryPoint(
                        (request, response, authException) -> response.sendError(HttpServletResponse.SC_UNAUTHORIZED)
                ))
                .authorizeHttpRequests((authorize) -> authorize
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()

                        .requestMatchers(HttpMethod.GET, "/actuator/health").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/inquiry/contact-submissions").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/user/auth/csrf").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/user/auth/login").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/user/auth/logout").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/user/auth/refresh").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/user/auth/register").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/user/change-password").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/user/request-otp").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/user/verify-otp").permitAll()
                        .requestMatchers("/error").permitAll()
                        .requestMatchers(HttpMethod.GET, "/health").permitAll()

                        .requestMatchers(HttpMethod.GET, "/api/admin/inquiries").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/api/admin/inquiries/delete").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.GET, "/api/admin/test").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.GET, "/api/admin/users").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/api/admin/users/delete").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/api/admin/users/update-profile").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/api/admin/users/update-role").hasRole("ADMIN")

                        .requestMatchers(HttpMethod.GET, "/api/user/auth").authenticated()

                        .anyRequest().authenticated()
                )
                .addFilterAfter(bearerTokenAuthFilter, BasicAuthenticationFilter.class);

        if (requireHttps) {
            http.redirectToHttps(Customizer.withDefaults());
        }

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();

        config.setAllowedOrigins(parseAllowedOrigins());
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        config.setAllowCredentials(true);
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }

    private List<String> parseAllowedOrigins() {
        return Arrays.stream(allowedOrigins.split(","))
                .map(String::trim)
                .filter(origin -> !origin.isBlank())
                .toList();
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

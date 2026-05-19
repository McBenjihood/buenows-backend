package com.buenws.buenws_backend.API.Configuration;

import com.buenws.buenws_backend.API.Exception.Custom.ExpiredTokenException;
import com.buenws.buenws_backend.API.Exception.Custom.ParseTokenException;
import com.buenws.buenws_backend.API.Service.Tokens.TokenService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.servlet.HandlerExceptionResolver;

import java.io.IOException;
import java.util.List;

@Component
public class BearerTokenAuthFilter extends OncePerRequestFilter {

    private final TokenService tokenService;
    private final HandlerExceptionResolver handlerExceptionResolver;

    public BearerTokenAuthFilter(TokenService tokenService, HandlerExceptionResolver handlerExceptionResolver) {
        this.tokenService = tokenService;
        this.handlerExceptionResolver = handlerExceptionResolver;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getServletPath();

        return path.equals("/api/user/auth/login")
                || path.equals("/api/user/auth/register")
                || path.equals("/api/user/auth/refresh")
                || path.equals("/api/user/auth/logout")
                || path.equals("/api/user/request-reset-password")
                || path.equals("/api/user/reset-password")
                || path.equals("/api/user/request-otp")
                || path.equals("/api/user/verify-otp")
                || path.equals("/api/inquiry/contact-submissions")
                || path.startsWith("/images/");
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        if (HttpMethod.OPTIONS.matches(request.getMethod())) {
            filterChain.doFilter(request, response);
            return;
        }

        String accessToken = tokenService.parseTokenFromHeader(request.getHeader("Authorization"));

        if (accessToken != null && !accessToken.isBlank()) {
            try {
                com.nimbusds.jwt.JWTClaimsSet claimsSet = tokenService.validateJWTToken(accessToken);
                String email = claimsSet.getSubject();

                List<String> roles = claimsSet.getStringListClaim("roles");
                if (roles == null) {
                    roles = List.of();
                }

                List<SimpleGrantedAuthority> authorities = roles.stream()
                        .map(role -> role.startsWith("ROLE_") ? role : "ROLE_" + role)
                        .map(SimpleGrantedAuthority::new)
                        .toList();

                Authentication authenticationToken =
                        new UsernamePasswordAuthenticationToken(email, null, authorities);

                SecurityContextHolder.getContext().setAuthentication(authenticationToken);

            } catch (Exception e) {
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);

                if (e instanceof ParseTokenException || e instanceof ExpiredTokenException) {
                    handlerExceptionResolver.resolveException(request, response, null, e);
                } else if (e instanceof java.text.ParseException) {
                    handlerExceptionResolver.resolveException(
                            request,
                            response,
                            null,
                            new ParseTokenException("Please Log in again.", "INVALID_TOKEN", e)
                    );
                } else {
                    handlerExceptionResolver.resolveException(request, response, null, e);
                }
                return;
            }
        }

        filterChain.doFilter(request, response);
    }
}
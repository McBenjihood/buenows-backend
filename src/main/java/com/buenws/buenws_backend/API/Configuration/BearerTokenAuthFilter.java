package com.buenws.buenws_backend.API.Configuration;


import com.buenws.buenws_backend.API.Exception.Custom.ExpiredTokenException;
import com.buenws.buenws_backend.API.Exception.Custom.ParseTokenException;
import com.buenws.buenws_backend.API.Service.Tokens.TokenService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
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

    TokenService tokenService;
    HandlerExceptionResolver handlerExceptionResolver;

    public BearerTokenAuthFilter(TokenService tokenService, HandlerExceptionResolver handlerExceptionResolver) {
        this.tokenService = tokenService;
        this.handlerExceptionResolver = handlerExceptionResolver;
    }

    @Override
    protected  void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        //Getting accesToken String
        String accessToken = tokenService.parseTokenFromHeader(request.getHeader("Authorization"));

        //Check if accessToken is valid
        if (accessToken != null && !accessToken.isBlank()){
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
                Authentication authenticationToken = new UsernamePasswordAuthenticationToken(email, null, authorities);
                SecurityContextHolder.getContext().setAuthentication(authenticationToken);
                
            } catch (Exception e) {
                // Return 401 and stop the filter chain immediately
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                
                if (e instanceof ParseTokenException || e instanceof ExpiredTokenException) {
                    handlerExceptionResolver.resolveException(request, response, null, e);
                } else if (e instanceof java.text.ParseException) {
                    handlerExceptionResolver.resolveException(request, response, null, new ParseTokenException("Please Log in again.", "INVALID_TOKEN", e));
                } else {
                    handlerExceptionResolver.resolveException(request, response, null, e);
                }
                return; // DO NOT call doFilter
            }
        }
        filterChain.doFilter(request, response);
    }
}

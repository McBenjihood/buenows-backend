package com.buenws.buenws_backend.API.Configuration;


import com.buenws.buenws_backend.API.Entity.UserEntity;
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
import java.util.Optional;

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
        if (!accessToken.isEmpty()){
            try {
                Optional<UserEntity> userEntity = tokenService.validateJWTToken(accessToken);
                if(userEntity.isEmpty()){
                    response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                }else {
                    UserEntity user = userEntity.get();
                    List<SimpleGrantedAuthority> authorities = user.getAuthorities().stream().map(SimpleGrantedAuthority::new).toList();
                    Authentication authenticationToken = new UsernamePasswordAuthenticationToken(user.getEmail(),null, authorities);
                    SecurityContextHolder.getContext().setAuthentication(authenticationToken);
                }
            }catch (ParseTokenException | ExpiredTokenException e) {
                handlerExceptionResolver.resolveException(request, response, null, e);
                return;
            }
        }
        filterChain.doFilter(request, response);
    }
}

package com.buenws.buenws_backend.api.configuration;


import com.buenws.buenws_backend.api.entity.UserEntity;
import com.buenws.buenws_backend.api.exception.customExceptions.ExpiredTokenException;
import com.buenws.buenws_backend.api.exception.customExceptions.ParseTokenException;
import com.buenws.buenws_backend.api.records.UserRecords;
import com.buenws.buenws_backend.api.service.tokens.TokenService;
import com.nimbusds.jose.JOSEException;
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
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.text.ParseException;
import java.util.Date;
import java.util.List;
import java.util.Optional;

@Component
public class BearerTokenAuthFilter extends OncePerRequestFilter {

    public BearerTokenAuthFilter(TokenService tokenService) {
        this.tokenService = tokenService;
    }

    TokenService tokenService;

    @Override
    protected  void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        String accessToken = tokenService.parseTokenFromHeader(request.getHeader("Authorization"));
        if (!accessToken.isEmpty()){
            try {
                Optional<UserEntity> userEntity = tokenService.validateToken(accessToken);

                if(userEntity.isEmpty()){
                    response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                }else {
                    UserEntity user = userEntity.get();
                    List<SimpleGrantedAuthority> authorities = user.getAuthorities().stream().map(SimpleGrantedAuthority::new).toList();
                    Authentication authenticationToken = new UsernamePasswordAuthenticationToken(user.getEmail(),null, authorities);
                    SecurityContextHolder.getContext().setAuthentication(authenticationToken);
                }
            } catch (JOSEException | ParseException e) {
                throw new ParseTokenException("Error processing Token");
            }
            catch (ExpiredTokenException e){
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                response.setContentType("application/json");

                byte[] responseBody = new ObjectMapper().writeValueAsBytes(new UserRecords.ExpiredTokenResponseRecord(
                        "TOKEN_EXPIRED",
                        "Token has expired"));
                response.getOutputStream().write(responseBody);
                return;
            }
        }
        filterChain.doFilter(request, response);
    }

}

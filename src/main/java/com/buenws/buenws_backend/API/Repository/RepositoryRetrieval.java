package com.buenws.buenws_backend.API.Repository;

import com.buenws.buenws_backend.API.Entity.ResetCodeEntity;
import com.buenws.buenws_backend.API.Entity.UserEntity;
import com.buenws.buenws_backend.API.Exception.Custom.InvalidUserException;
import com.buenws.buenws_backend.API.Repository.Repositories.ResetCodeRepository;
import com.buenws.buenws_backend.API.Repository.Repositories.UserRepository;
import com.buenws.buenws_backend.API.Service.Tokens.TokenService;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class RepositoryRetrieval {

    private final TokenService tokenService;
    private final UserRepository userRepository;
    private final ResetCodeRepository resetCodeRepository;

    public RepositoryRetrieval(TokenService tokenService, UserRepository userRepository, ResetCodeRepository resetCodeRepository) {
        this.tokenService = tokenService;
        this.userRepository = userRepository;
        this.resetCodeRepository = resetCodeRepository;
    }

    public UserEntity getUserEntityFromToken(String token){
        com.nimbusds.jwt.JWTClaimsSet claimsSet = tokenService.validateJWTToken(token);
        Optional<UserEntity> userEntity = userRepository.findByEmail(claimsSet.getSubject());
        if(userEntity.isPresent()){
            return userEntity.get();
        }else {
            throw new InvalidUserException("User not found.", "INVALID_USER");
        }
    }

    public UserEntity getUserEntityFromEmail(String email){
        Optional<UserEntity> userEntity = userRepository.findByEmail(email);
        if(userEntity.isPresent()){
            return userEntity.get();
        }else {
            throw new InvalidUserException("User not found.", "INVALID_USER");
        }
    }

    public ResetCodeEntity getResetCodeEntityFromVerified_Token(String verified_token){
        Optional<ResetCodeEntity> resetCodeEntity = resetCodeRepository.findByVerifiedToken(verified_token);
        if(resetCodeEntity.isPresent()){
            return resetCodeEntity.get();
        }else {
            throw new InvalidUserException("Error verifying Users token.", "INVALID_USER");
        }
    }
}

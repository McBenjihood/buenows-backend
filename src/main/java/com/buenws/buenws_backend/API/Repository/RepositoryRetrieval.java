package com.buenws.buenws_backend.API.Repository;

import com.buenws.buenws_backend.API.Entity.OTPAuthEntity;
import com.buenws.buenws_backend.API.Entity.UserEntity;
import com.buenws.buenws_backend.API.Exception.Custom.InvalidUserException;
import com.buenws.buenws_backend.API.Repository.Repositories.OTPAuthRepository;
import com.buenws.buenws_backend.API.Repository.Repositories.UserRepository;
import com.buenws.buenws_backend.API.Service.Tokens.TokenService;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

@Component
public class RepositoryRetrieval {

    private final TokenService tokenService;
    private final UserRepository userRepository;
    private final OTPAuthRepository OTPAuthRepository;

    public RepositoryRetrieval(TokenService tokenService, UserRepository userRepository, OTPAuthRepository OTPAuthRepository) {
        this.tokenService = tokenService;
        this.userRepository = userRepository;
        this.OTPAuthRepository = OTPAuthRepository;
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

    public OTPAuthEntity getResetCodeEntityFromVerified_Token(UUID verified_token){
        Optional<OTPAuthEntity> resetCodeEntity = OTPAuthRepository.findByVerifiedToken(verified_token);
        if(resetCodeEntity.isPresent()){
            return resetCodeEntity.get();
        }else {
            throw new InvalidUserException("Error verifying Users token.", "INVALID_USER");
        }
    }

    public OTPAuthEntity getResetCodeEntityFromContact_Information(String contact_information){
        Optional<OTPAuthEntity> resetCodeEntity = OTPAuthRepository.findByContact(contact_information);
        if (resetCodeEntity.isPresent()){
            return  resetCodeEntity.get();
        }else {
            throw new InvalidUserException("Error verifying Users token.", "INVALID_USER");
        }
    }
}
